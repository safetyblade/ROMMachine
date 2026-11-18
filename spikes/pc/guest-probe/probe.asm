bits 16
org 100h
; Original DOS workload: persistent marker, keyboard, mouse and two real optical reads.
push cs
pop ds
mov dx, greeting
mov ah, 9
int 21h
mov dx, filename
mov ax, 3d00h
int 21h
jc first
mov bx, ax
mov dx, value
mov cx, 1
mov ah, 3fh
int 21h
mov ah, 3eh
int 21h
first:
inc byte [value]
mov dx, filename
xor cx, cx
mov ah, 3ch
int 21h
jc fail
mov bx, ax
mov cx, 1
mov dx, value
mov ah, 40h
int 21h
mov ah, 3eh
int 21h
xor ax, ax
int 33h
mov [mouse_installed], al
mov dx, prompt
mov ah, 9
int 21h
xor ah, ah
int 16h
mov [key], al
mov si, cd_first
call read_cd
cmp byte [mouse_installed], 0
je second_disc
mov dx, click_prompt
mov ah, 9
int 21h
wait_click:
mov ax, 3
int 33h
test bx, 1
jz wait_click
mov byte [clicked], 1
second_disc:
mov dx, switch_prompt
mov ah, 9
int 21h
wait_second:
xor ah, ah
int 16h
cmp al, 'b'
je read_second
cmp al, 'B'
jne wait_second
read_second:
mov [key2], al
mov si, cd_second
call read_cd
mov dx, results
xor cx, cx
mov ah, 3ch
int 21h
jc fail
mov bx, ax
mov dx, value
mov cx, 9
mov ah, 40h
int 21h
mov ah, 3eh
int 21h
mov dx, done
mov ah, 9
int 21h
mov ax, 4c00h
int 21h
read_cd:
; SI points to two output bytes: actual optical byte and DOS error.
mov dx, optical
mov ax, 3d00h
int 21h
jc read_open_error
mov bx, ax
mov dx, si
mov cx, 1
mov ah, 3fh
int 21h
jc read_error
cmp ax, 1
je close_cd
mov byte [si+1], 0ffh
jmp close_cd
read_error:
mov [si+1], al
close_cd:
mov ah, 3eh
int 21h
ret
read_open_error:
mov [si+1], al
ret
fail:
mov ax, 4c01h
int 21h
filename db 'PCSTATE.DAT',0
optical db 'D:\DISC.TXT',0
results db 'PCINPUT.DAT',0
value db 0
key db 0
mouse_installed db 0
clicked db 0
key2 db 0
cd_first db 0,0
cd_second db 0,0
greeting db 'CARTDECK PC probe',13,10,'$'
prompt db 'Mount first disc, then press A.',13,10,'$'
click_prompt db 'Click the left mouse button.',13,10,'$'
switch_prompt db 'Switch disc, then press B.',13,10,'$'
done db 'Probe written. Shut down or exit guest before reopening.',13,10,'$'
