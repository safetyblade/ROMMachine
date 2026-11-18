bits 16
org 100h
; Original DOS workload: persistent marker, keyboard, mouse and optical read.
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
mov dx, optical
mov ax, 3d00h
int 21h
jc no_cd
mov bx, ax
mov dx, cdvalue
mov cx, 1
mov ah, 3fh
int 21h
mov ah, 3eh
int 21h
no_cd:
xor ax, ax
int 33h
mov [mouse_installed], al
mov dx, prompt
mov ah, 9
int 21h
xor ah, ah
int 16h
mov [key], al
cmp byte [mouse_installed], 0
je write_results
mov dx, click_prompt
mov ah, 9
int 21h
wait_click:
mov ax, 3
int 33h
test bx, 1
jz wait_click
mov byte [clicked], 1
write_results:
mov dx, results
xor cx, cx
mov ah, 3ch
int 21h
jc fail
mov bx, ax
mov dx, key
mov cx, 4
mov ah, 40h
int 21h
mov ah, 3eh
int 21h
mov dx, done
mov ah, 9
int 21h
mov ax, 4c00h
int 21h
fail:
mov ax, 4c01h
int 21h
filename db 'PCSTATE.DAT',0
optical db 'D:\DISC.TXT',0
results db 'PCINPUT.DAT',0
value db 0
key db 0
mouse_installed db 0
cdvalue db 0
clicked db 0
greeting db 'CARTDECK PC probe',13,10,'$'
prompt db 'Press a key to retain the result.',13,10,'$'
click_prompt db 'Click the left mouse button.',13,10,'$'

done db 'Probe written. Shut down or exit guest before reopening.',13,10,'$'
