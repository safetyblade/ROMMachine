/* Original no-libc x86_64 Linux executable: no network, no user content.
 * Append a fixed record to a private relative file, then print success.
 */
.section .rodata
path: .asciz "probe-progress.txt"
record: .ascii "CARTDECK_BOX64_OK\n"
.set length, .-record
.section .text
.global _start
_start:
    mov $257,%eax
    mov $-100,%edi
    lea path(%rip),%rsi
    mov $1089,%edx
    mov $384,%r10d
    syscall
    test %rax,%rax
    js failed
    mov %eax,%edi
    mov $1,%eax
    lea record(%rip),%rsi
    mov $length,%edx
    syscall
    cmp $length,%rax
    jne failed
    mov $3,%eax
    syscall
    test %rax,%rax
    jne failed
    mov $1,%eax
    mov $1,%edi
    lea record(%rip),%rsi
    mov $length,%edx
    syscall
    cmp $length,%rax
    jne failed
    mov $37,%edi
    jmp finish
failed:
    mov $90,%edi
finish:
    mov $60,%eax
    syscall
