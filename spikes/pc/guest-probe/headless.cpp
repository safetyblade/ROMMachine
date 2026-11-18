// SPDX-License-Identifier: GPL-2.0-or-later
// Actual core execution under a bounded Linux libretro frontend; NOT Android device proof.
#include <libretro.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
#include <cstdarg>
#include <cstring>
#include <string>
#include <stdexcept>

static std::string systemDir, saveDir;
static retro_keyboard_event_t keyboard = nullptr;
static retro_disk_control_callback disks{};
static retro_disk_control_ext_callback extended{};
static unsigned discFirst=0, discSecond=1;
static unsigned heldKey=0;
static bool firstMounted=false;
static unsigned frames = 0, polls = 0, rendered = 0;
static bool keyDown = false, mouseDown = false;
static bool switched = false;
static void logger(retro_log_level, const char* fmt, ...) {
    va_list args; va_start(args,fmt); vfprintf(stderr,fmt,args); va_end(args);
}
static bool env(unsigned cmd, void* data) {
    switch(cmd) {
    case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY: *static_cast<const char**>(data)=systemDir.c_str(); return true;
    case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY: *static_cast<const char**>(data)=saveDir.c_str(); return true;
    case RETRO_ENVIRONMENT_GET_LOG_INTERFACE: static_cast<retro_log_callback*>(data)->log=logger; return true;
    case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION: *static_cast<unsigned*>(data)=0; return true;
    case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE: *static_cast<bool*>(data)=false; return true;
    case RETRO_ENVIRONMENT_GET_CAN_DUPE: *static_cast<bool*>(data)=true; return true;
    case RETRO_ENVIRONMENT_GET_FASTFORWARDING: *static_cast<bool*>(data)=false; return true;
    case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: return true;
    case RETRO_ENVIRONMENT_SET_VARIABLES: return true;
    case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK: keyboard=static_cast<retro_keyboard_callback*>(data)->callback; return true;
    case RETRO_ENVIRONMENT_SET_DISK_CONTROL_EXT_INTERFACE:
        extended=*static_cast<retro_disk_control_ext_callback*>(data);
        disks={extended.set_eject_state,extended.get_eject_state,extended.get_image_index,
            extended.set_image_index,extended.get_num_images,extended.replace_image_index,extended.add_image_index};
        return true;
    case RETRO_ENVIRONMENT_SET_DISK_CONTROL_INTERFACE: disks=*static_cast<retro_disk_control_callback*>(data); return true;
    default: return false;
    }
}
static void video(const void* pixels,unsigned,unsigned,size_t){ if(pixels)++rendered; }
static void audio(int16_t,int16_t){}
static size_t batch(const int16_t*,size_t n){return n;}
static void poll(){++polls;}
static int16_t input(unsigned port,unsigned device,unsigned index,unsigned id){
    if(port!=0||index!=0)return 0;
    if(device==RETRO_DEVICE_KEYBOARD&&id==heldKey)return keyDown;
    if(device==RETRO_DEVICE_MOUSE){
        if(id==RETRO_DEVICE_ID_MOUSE_LEFT)return mouseDown;
        if(id==RETRO_DEVICE_ID_MOUSE_X)return mouseDown?3:0;
    }
    return 0;
}
template<class T> static T load(void* lib,const char* name) {
    auto p=dlsym(lib,name);if(!p)throw std::runtime_error(name);return reinterpret_cast<T>(p);
}
int main(int argc,char**argv){
    if(argc!=5)return 2;
    systemDir=argv[3];saveDir=argv[4];
    void* lib=dlopen(argv[1],RTLD_NOW|RTLD_LOCAL);
    if(!lib){fprintf(stderr,"%s\n",dlerror());return 2;}
#define FUN(name) auto name = load<decltype(&::name)>(lib,#name)
    FUN(retro_set_environment);FUN(retro_set_video_refresh);FUN(retro_set_audio_sample);
    FUN(retro_set_audio_sample_batch);FUN(retro_set_input_poll);FUN(retro_set_input_state);
    FUN(retro_init);FUN(retro_deinit);FUN(retro_load_game);FUN(retro_unload_game);FUN(retro_run);
    FUN(retro_api_version);FUN(retro_set_controller_port_device);
    if(retro_api_version()!=RETRO_API_VERSION)return 2;
    retro_set_environment(env);retro_set_video_refresh(video);retro_set_audio_sample(audio);
    retro_set_audio_sample_batch(batch);retro_set_input_poll(poll);retro_set_input_state(input);
    retro_init();
    retro_game_info game{};game.path=argv[2];
    if(!retro_load_game(&game)){fprintf(stderr,"retro_load_game rejected probe\n");retro_deinit();return 3;}
    retro_set_controller_port_device(0,RETRO_DEVICE_JOYPAD);
    auto mount=[&](unsigned which) {
        if (!disks.get_num_images || !disks.set_eject_state || !disks.get_eject_state ||
            !disks.set_image_index || !disks.get_image_index || which>=disks.get_num_images()) return false;
        const bool ok=disks.set_eject_state(true)&&disks.get_eject_state()&&disks.set_image_index(which)&&
            disks.set_eject_state(false)&&!disks.get_eject_state()&&disks.get_image_index()==which;
        fprintf(stderr,"mount index=%u result=%d ejected=%d\n",which,ok,disks.get_eject_state());
        return ok;
    };
    // Actual guest is blocked on A until the first disc is mounted. B follows a real switch.
    for(frames=0;frames<900;++frames){
        if(frames==30 && disks.get_num_images){
            unsigned count=disks.get_num_images();
            fprintf(stderr,"optical image count=%u\n",count);
            for(unsigned i=0;i<count && extended.get_image_path;++i){
                char path[4096]={}; if(!extended.get_image_path(i,path,sizeof(path)))continue;
                fprintf(stderr,"optical image %u: %s\n",i,path);
                std::string lower(path);for(char &c:lower)if(c>='A'&&c<='Z')c+=32;
                if(lower.find("disc1.iso")!=std::string::npos)discFirst=i;
                if(lower.find("disc2.iso")!=std::string::npos)discSecond=i;
            }
            firstMounted=mount(discFirst);
        }
        if(frames==500)switched=discFirst!=discSecond&&mount(discSecond);
        if(frames==60 || frames==600 || frames==720){
            heldKey=frames==60?RETROK_a:RETROK_b;
            keyDown=true;if(keyboard)keyboard(true,heldKey,heldKey==RETROK_a?'a':'b',0);
        }
        if(frames==75 || frames==615 || frames==735){
            if(keyDown&&keyboard)keyboard(false,heldKey,0,0);keyDown=false;
        }
        mouseDown=(frames%120>=90 && frames%120<110);
        retro_run();
    }
    if(keyDown&&keyboard)keyboard(false,heldKey,0,0);
    retro_unload_game();retro_deinit();
    printf("frames=%u rendered=%u polls=%u keyboard_callback=%d optical_switch=%d\n",frames,rendered,polls,keyboard!=nullptr,switched);
    return (rendered&&polls&&keyboard&&firstMounted&&switched)?0:4;
}
