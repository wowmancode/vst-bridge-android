#include "vst2_editor.h"

#include <vst.h>

#include <cstdint>
#include <iostream>
#include <string>

#include <windows.h>

namespace {

vst_effect_t* activeEffect = nullptr;

intptr_t VST_FUNCTION_INTERFACE hostCallback(
    vst_effect_t*, int32_t opcode, int32_t index, int64_t value, const char*, float)
{
    switch (opcode)
    {
        case VST_HOST_OPCODE_VST_VERSION: return VST_VERSION_2_4_0_0;
        case VST_HOST_OPCODE_GET_SAMPLE_RATE: return 48000;
        case VST_HOST_OPCODE_GET_BLOCK_SIZE: return 256;
        case VST_HOST_OPCODE_EDITOR_RESIZE:
        {
            HWND window = GetActiveWindow();
            if (!window || index <= 0 || value <= 0) return 0;
            RECT rect{0, 0, index, static_cast<LONG>(value)};
            AdjustWindowRect(&rect, WS_OVERLAPPEDWINDOW, FALSE);
            SetWindowPos(window, nullptr, 0, 0, rect.right - rect.left,
                         rect.bottom - rect.top, SWP_NOMOVE | SWP_NOZORDER);
            return 1;
        }
        default: return 0;
    }
}

LRESULT CALLBACK windowProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam)
{
    if (message == WM_TIMER && activeEffect)
    {
        activeEffect->control(activeEffect, VST_EFFECT_OPCODE_EDITOR_KEEP_ALIVE,
                              0, 0, nullptr, 0.f);
        return 0;
    }
    if (message == WM_CLOSE)
    {
        DestroyWindow(window);
        return 0;
    }
    if (message == WM_DESTROY)
    {
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(window, message, wParam, lParam);
}

} // namespace

int openVst2Editor(const std::string& pluginId, const wchar_t* path)
{
    HMODULE module = LoadLibraryW(path);
    if (!module)
    {
        std::cerr << "Could not load VST2 DLL (Windows error " << GetLastError() << ")\n";
        return 20;
    }

    using EntryPoint = vst_effect_t* (VST_FUNCTION_INTERFACE*)(vst_host_callback_t);
    auto entry = reinterpret_cast<EntryPoint>(GetProcAddress(module, "VSTPluginMain"));
    if (!entry) entry = reinterpret_cast<EntryPoint>(GetProcAddress(module, "main"));
    activeEffect = entry ? entry(hostCallback) : nullptr;
    if (!activeEffect || activeEffect->magic_number != static_cast<std::int32_t>(VST_MAGICNUMBER)
        || !activeEffect->control)
    {
        std::cerr << "The DLL is not a valid VST2 effect\n";
        activeEffect = nullptr;
        FreeLibrary(module);
        return 21;
    }
    if (!(activeEffect->flags & VST_EFFECT_FLAG_EDITOR))
    {
        std::cerr << "This VST2 plug-in does not provide an editor\n";
        activeEffect = nullptr;
        FreeLibrary(module);
        return 22;
    }

    activeEffect->control(activeEffect, VST_EFFECT_OPCODE_INITIALIZE, 0, 0, nullptr, 0.f);
    vst_rect_t* editorRect = nullptr;
    activeEffect->control(activeEffect, VST_EFFECT_OPCODE_EDITOR_GET_RECT,
                          0, 0, &editorRect, 0.f);
    const int width = editorRect ? editorRect->right - editorRect->left : 800;
    const int height = editorRect ? editorRect->bottom - editorRect->top : 600;

    const wchar_t* className = L"VstBridgeEditorWindow";
    WNDCLASSW windowClass{};
    windowClass.lpfnWndProc = windowProc;
    windowClass.hInstance = GetModuleHandleW(nullptr);
    windowClass.lpszClassName = className;
    windowClass.hCursor = LoadCursor(nullptr, IDC_ARROW);
    RegisterClassW(&windowClass);

    RECT outer{0, 0, width, height};
    AdjustWindowRect(&outer, WS_OVERLAPPEDWINDOW, FALSE);
    const std::wstring title = L"VST Bridge - "
        + std::wstring(pluginId.begin(), pluginId.end());
    HWND window = CreateWindowExW(0, className, title.c_str(), WS_OVERLAPPEDWINDOW,
                                  CW_USEDEFAULT, CW_USEDEFAULT,
                                  outer.right - outer.left, outer.bottom - outer.top,
                                  nullptr, nullptr, windowClass.hInstance, nullptr);
    if (!window)
    {
        activeEffect->control(activeEffect, VST_EFFECT_OPCODE_DESTROY, 0, 0, nullptr, 0.f);
        activeEffect = nullptr;
        FreeLibrary(module);
        return 23;
    }

    activeEffect->control(activeEffect, VST_EFFECT_OPCODE_EDITOR_OPEN,
                          0, 0, window, 0.f);
    ShowWindow(window, SW_SHOW);
    SetTimer(window, 1, 30, nullptr);

    MSG message{};
    while (GetMessageW(&message, nullptr, 0, 0) > 0)
    {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }

    activeEffect->control(activeEffect, VST_EFFECT_OPCODE_EDITOR_CLOSE, 0, 0, nullptr, 0.f);
    activeEffect->control(activeEffect, VST_EFFECT_OPCODE_DESTROY, 0, 0, nullptr, 0.f);
    activeEffect = nullptr;
    FreeLibrary(module);
    return 0;
}
