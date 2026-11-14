"""Validate the loader entry in readelf's dynamic symbol table."""


def require_jni_entry(symbols):
    for line in symbols.splitlines():
        fields = line.split()
        if (len(fields) >= 8 and fields[3] == 'FUNC' and
                fields[4] in {'GLOBAL', 'WEAK'} and fields[5] == 'DEFAULT' and
                fields[6] not in {'UND', 'ABS'} and
                fields[7].split('@', 1)[0] == 'JNI_OnLoad'):
            return
    raise ValueError('Xbox 360: missing defined, exported JNI_OnLoad function')
