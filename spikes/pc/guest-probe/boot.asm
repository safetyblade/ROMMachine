bits 16
org 7c00h
; Original BIOS program: require sector read, increment retained byte, write and halt.
cli
xor ax, ax
mov ds, ax
mov es, ax
mov ss, ax
mov sp, 7c00h
sti
; The pinned core BOOT command sets EDX=0 even for C:; this fixture is a known HDD.
mov byte [drive], 80h
mov bx, 7e00h
mov ax, 0201h
xor ch, ch
mov cl, 2
xor dh, dh
mov dl, [drive]
int 13h
jc failed
cmp dword [7e00h], 'PCEM'
je update
cmp dword [7e00h], 'PCBO'
jne failed
update:
inc byte [7e08h]
cld
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
success db 'PCBOOTOK: persistent sector updated.',0
failure db 'PC disk read/write failed.',0
times 510-($-$$) db 0
dw 0aa55h
