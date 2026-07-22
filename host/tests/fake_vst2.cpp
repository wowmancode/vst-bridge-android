#include <vst.h>

#include <cstring>

#include <windows.h>

namespace {

vst_effect_t effect {};
vst_rect_t editorRect {0, 0, 360, 640};

void copyText (void* destination, std::size_t capacity, const char* source)
{
    if (destination)
        strncpy_s (static_cast<char*> (destination), capacity, source, _TRUNCATE);
}

intptr_t VST_FUNCTION_INTERFACE dispatch (
    vst_effect_t*, int32_t opcode, int32_t, intptr_t, void* pointer, float)
{
    switch (opcode)
    {
        case VST_EFFECT_OPCODE_EDITOR_GET_RECT:
            if (pointer)
                *static_cast<vst_rect_t**> (pointer) = &editorRect;
            return 1;
        case VST_EFFECT_OPCODE_EFFECT_NAME:
            copyText (pointer, VST_BUFFER_SIZE_EFFECT_NAME, "Bridge Test Effect");
            return 1;
        case VST_EFFECT_OPCODE_VENDOR_NAME:
            copyText (pointer, VST_BUFFER_SIZE_VENDOR_NAME, "VST Bridge");
            return 1;
        case VST_EFFECT_OPCODE_PRODUCT_NAME:
            copyText (pointer, VST_BUFFER_SIZE_PRODUCT_NAME, "CI Test Plugin");
            return 1;
        case VST_EFFECT_OPCODE_VENDOR_VERSION: return 100;
        case VST_EFFECT_OPCODE_VST_VERSION: return VST_VERSION_2_4_0_0;
        case VST_EFFECT_OPCODE_CATEGORY: return VST_EFFECT_CATEGORY_EFFECT;
        default: return 0;
    }
}

} // namespace

extern "C" __declspec(dllexport) vst_effect_t* VSTPluginMain (vst_host_callback_t)
{
    effect.magic_number = static_cast<int32_t> (VST_MAGICNUMBER);
    effect.control = dispatch;
    effect.num_programs = 1;
    effect.num_params = 3;
    effect.num_inputs = 2;
    effect.num_outputs = 2;
    effect.flags = VST_EFFECT_FLAG_SUPPORTS_FLOAT | VST_EFFECT_FLAG_EDITOR;
    effect.unique_id = static_cast<int32_t> (VST_FOURCC ('V', 'B', 'T', 'S'));
    effect.version = 100;
    return &effect;
}

BOOL WINAPI DllMain (HINSTANCE, DWORD, LPVOID)
{
    return TRUE;
}
