/* Original integrated PE32 acceptance cartridge, not proprietary game content. */
#include <windows.h>
#include <mmsystem.h>
#include <stdio.h>
#include <stdlib.h>

static unsigned int count, clicks, keys;
static int sound = -1;
static WAVEHDR header;
static HWAVEOUT wave;
static short samples[8000];
static void test_sound(void) {
    if(wave)return;
    WAVEFORMATEX fmt={WAVE_FORMAT_PCM,1,8000,16000,2,16,0};
    sound=(int)waveOutOpen(&wave,WAVE_MAPPER,&fmt,0,0,CALLBACK_NULL);
    if(sound!=MMSYSERR_NOERROR){wave=NULL;return;}
    for(unsigned i=0;i<8000;i++)samples[i]=(i%40<20?2500:-2500);
    header.lpData=(char*)samples;header.dwBufferLength=sizeof(samples);
    sound=(int)waveOutPrepareHeader(wave,&header,sizeof(header));
    if(sound==MMSYSERR_NOERROR)sound=(int)waveOutWrite(wave,&header,sizeof(header));
}
static LRESULT CALLBACK window_proc(HWND hwnd,UINT msg,WPARAM w,LPARAM l){
    if(msg==WM_LBUTTONDOWN){clicks++;SetFocus(hwnd);InvalidateRect(hwnd,NULL,TRUE);return 0;}
    if(msg==WM_KEYDOWN){
        if(w=='K'){keys++;InvalidateRect(hwnd,NULL,TRUE);}
        if(w=='A'){test_sound();InvalidateRect(hwnd,NULL,TRUE);}
        if(w=='S'&&clicks&&keys){
            FILE *file=fopen("wine-progress.txt","w");
            if(file){fprintf(file,"%u\n",++count);fclose(file);clicks=keys=0;InvalidateRect(hwnd,NULL,TRUE);}
        }
        return 0;
    }
    if(msg==WM_PAINT){
        PAINTSTRUCT paint;HDC dc=BeginPaint(hwnd,&paint);
        const char *lines[]={"CARTDECK Wine / PE32 verification","Click in this window, then press K.",
          "Press S to save; close and reopen to verify count.","Press A for one-second audio test."};
        for(int i=0;i<4;i++)TextOutA(dc,15,15+28*i,lines[i],lstrlenA(lines[i]));
        char status[180];snprintf(status,sizeof(status),"Saved count: %u | clicks: %u | keys: %u | audio result: %d",count,clicks,keys,sound);
        TextOutA(dc,15,145,status,lstrlenA(status));EndPaint(hwnd,&paint);return 0;
    }
    if(msg==WM_DESTROY){
        if(wave){waveOutReset(wave);waveOutUnprepareHeader(wave,&header,sizeof(header));waveOutClose(wave);}
        PostQuitMessage(0);return 0;
    }
    return DefWindowProcA(hwnd,msg,w,l);
}
int main(void){
    FILE *file=fopen("wine-progress.txt","r");if(file){fscanf(file,"%u",&count);fclose(file);}
    WNDCLASSA cls={0};cls.lpfnWndProc=window_proc;cls.hInstance=GetModuleHandleA(NULL);
    cls.lpszClassName="CartdeckWineProbe";cls.hbrBackground=(HBRUSH)(COLOR_WINDOW+1);cls.hCursor=LoadCursor(NULL,IDC_ARROW);
    if(!RegisterClassA(&cls))return 10;
    HWND window=CreateWindowA(cls.lpszClassName,"CARTDECK • Wine PE32",WS_OVERLAPPEDWINDOW|WS_VISIBLE,
        CW_USEDEFAULT,CW_USEDEFAULT,630,300,NULL,NULL,cls.hInstance,NULL);
    if(!window)return 11;
    MSG message;while(GetMessageA(&message,NULL,0,0)>0){TranslateMessage(&message);DispatchMessageA(&message);}
    return 0;
}
