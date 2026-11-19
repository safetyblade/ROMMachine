/* Original PC-R06 Win32/Win64 fixture. No proprietary game or OS content. */
#include <windows.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static LRESULT CALLBACK wndproc(HWND hwnd, UINT msg, WPARAM w, LPARAM l) {
    return DefWindowProcA(hwnd,msg,w,l);
}
int main(void) {
    char cwd[MAX_PATH], value[32];
    DWORD size=sizeof(value);
    unsigned int counter=0;
    HKEY key;
    if(!GetCurrentDirectoryA(sizeof(cwd),cwd)) return 10;
    if(RegCreateKeyExA(HKEY_CURRENT_USER,"Software\\CARTDECK\\PcR06",0,NULL,0,
                      KEY_READ|KEY_WRITE,NULL,&key,NULL)!=ERROR_SUCCESS) return 11;
    DWORD type=0;
    LONG rc=RegQueryValueExA(key,"Counter",0,&type,(BYTE*)value,&size);
    if(rc==ERROR_SUCCESS){if(type!=REG_SZ||size<2||size>sizeof(value))return 12;counter=(unsigned)atoi(value);}
    else if(rc!=ERROR_FILE_NOT_FOUND)return 13;
    unsigned int diskCounter=0;
    FILE *prior=fopen("progress.txt","r");
    if(prior){if(fscanf(prior,"%u",&diskCounter)!=1)return 14;fclose(prior);}
    if(diskCounter!=counter)return 15;
    WNDCLASSA cls={0};cls.lpfnWndProc=wndproc;cls.hInstance=GetModuleHandleA(NULL);cls.lpszClassName="CartdeckProbe";
    if(!RegisterClassA(&cls))return 16;
    HWND window=CreateWindowExA(0,cls.lpszClassName,"CARTDECK PC-R06",WS_OVERLAPPEDWINDOW,
                               0,0,640,480,NULL,NULL,cls.hInstance,NULL);
    if(!window)return 17;
    ShowWindow(window,SW_SHOW);UpdateWindow(window);
    HDC dc=GetDC(window);if(!dc)return 18;
    if(!TextOutA(dc,20,20,"CARTDECK",8))return 19;
    ReleaseDC(window,dc);DestroyWindow(window);
    counter++;
    snprintf(value,sizeof(value),"%u",counter);
    if(RegSetValueExA(key,"Counter",0,REG_SZ,(BYTE*)value,(DWORD)strlen(value)+1)!=ERROR_SUCCESS)return 20;
    if(RegFlushKey(key)!=ERROR_SUCCESS)return 21;
    RegCloseKey(key);
    /* Binary mode makes the persisted counter format identical on all hosts. */
    FILE *out=fopen("progress.txt","wb");
    if(!out)return 22;
    if(fprintf(out,"%u\n",counter)<0||fclose(out)!=0)return 23;
    printf("PC_R06_WINDOWS_OK bits=%u counter=%u\n",(unsigned)(sizeof(void*)*8),counter);
    return 0;
}
