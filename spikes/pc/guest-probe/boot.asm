bits 16
org 7c00h
; Original synthetic BIOS workload. Writes sector 2 (no commercial OS).
cli
xor ax, ax
mov ds, ax
mov es, ax
mov ss, ax
mov sp, 7c00h
sti
mov [drive], dl
mov di, 7e00h
mov cx, 256
xor ax, ax
rep stosw
mov si, marker
mov di, 7e00h
mov cx, 8
rep movsb
mov bx, 7e00h
mov ax, 0301h
xor ch, ch
mov cl, 2
xor dh, dh
mov dl, [drive]
int 13h
jc failed
mov si, success
jmp print
failed: mov si, failure
print:
lodsb
test al, al
jz stop
mov ah, 0eh
xor bx, bx
int 10h
jmp print
stop: cli
hlt
jmp stop
drive db 0
marker db 'PCBOOTOK'
success db 'PCBOOTOK: sector written. Reopen and inspect disk.',0
failure db 'PC disk write failed.',0
times 510-($-$$) db 0
dw 0aa55h
