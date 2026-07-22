#include "vst2_scanner.h"

#include <vst.h>

#include <cstdint>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>

#include <windows.h>

namespace {

constexpr std::uint32_t kProtocolVersion = 1;

std::string jsonEscape (const std::string& value)
{
    static constexpr char hex[] = "0123456789abcdef";
    std::string result;
    result.reserve (value.size () + 8);
    for (const unsigned char character : value)
    {
        switch (character)
        {
            case '"': result += "\\\""; break;
            case '\\': result += "\\\\"; break;
            case '\b': result += "\\b"; break;
            case '\f': result += "\\f"; break;
            case '\n': result += "\\n"; break;
            case '\r': result += "\\r"; break;
            case '\t': result += "\\t"; break;
            default:
                if (character < 0x20)
                {
                    result += "\\u00";
                    result += hex[(character >> 4) & 0xf];
                    result += hex[character & 0xf];
                }
                else
                {
                    result += static_cast<char> (character);
                }
        }
    }
    return result;
}

void emitState (const std::string& pluginId, std::uint64_t sequence, const char* state)
{
    std::cout << "{\"protocolVersion\":" << kProtocolVersion
              << ",\"pluginId\":\"" << jsonEscape (pluginId)
              << "\",\"sequence\":" << sequence
              << ",\"type\":\"state\",\"state\":\"" << state << "\"}\n";
}

void emitError (const std::string& pluginId, std::uint64_t sequence,
                const char* code, const std::string& message)
{
    std::cout << "{\"protocolVersion\":" << kProtocolVersion
              << ",\"pluginId\":\"" << jsonEscape (pluginId)
              << "\",\"sequence\":" << sequence
              << ",\"type\":\"error\",\"code\":\"" << code
              << "\",\"message\":\"" << jsonEscape (message) << "\"}\n";
}

intptr_t VST_FUNCTION_INTERFACE hostCallback (
    vst_effect_t*, int32_t opcode, int32_t, int64_t, const char*, float)
{
    switch (opcode)
    {
        case VST_HOST_OPCODE_VST_VERSION: return VST_VERSION_2_4_0_0;
        case VST_HOST_OPCODE_GET_SAMPLE_RATE: return 48000;
        case VST_HOST_OPCODE_GET_BLOCK_SIZE: return 256;
        default: return 0;
    }
}

std::string windowsError (DWORD code)
{
    char* message = nullptr;
    const DWORD length = FormatMessageA (
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr, code, MAKELANGID (LANG_NEUTRAL, SUBLANG_DEFAULT),
        reinterpret_cast<char*> (&message), 0, nullptr);
    std::string result = length && message ? std::string (message, length) : "Windows error " + std::to_string (code);
    if (message)
        LocalFree (message);
    while (!result.empty () && (result.back () == '\r' || result.back () == '\n'))
        result.pop_back ();
    return result;
}

std::string effectString (vst_effect_t* effect, VST_EFFECT_OPCODE opcode, std::size_t capacity)
{
    std::string value (capacity, '\0');
    effect->control (effect, opcode, 0, 0, value.data (), 0.f);
    value.resize (std::char_traits<char>::length (value.c_str ()));
    return value;
}

std::string fourCc (std::int32_t value)
{
    std::string result (4, ' ');
    result[0] = static_cast<char> ((value >> 24) & 0xff);
    result[1] = static_cast<char> ((value >> 16) & 0xff);
    result[2] = static_cast<char> ((value >> 8) & 0xff);
    result[3] = static_cast<char> (value & 0xff);
    for (char& character : result)
    {
        if (static_cast<unsigned char> (character) < 0x20 ||
            static_cast<unsigned char> (character) > 0x7e)
            character = '?';
    }
    return result;
}

void emitMetadata (const std::string& pluginId, std::uint64_t sequence, vst_effect_t* effect)
{
    const std::string name = effectString (effect, VST_EFFECT_OPCODE_EFFECT_NAME,
                                           VST_BUFFER_SIZE_EFFECT_NAME + 1);
    const std::string vendor = effectString (effect, VST_EFFECT_OPCODE_VENDOR_NAME,
                                             VST_BUFFER_SIZE_VENDOR_NAME + 1);
    const std::string product = effectString (effect, VST_EFFECT_OPCODE_PRODUCT_NAME,
                                              VST_BUFFER_SIZE_PRODUCT_NAME + 1);
    const auto category = effect->control (effect, VST_EFFECT_OPCODE_CATEGORY, 0, 0, nullptr, 0.f);
    const auto apiVersion = effect->control (effect, VST_EFFECT_OPCODE_VST_VERSION, 0, 0, nullptr, 0.f);
    const auto vendorVersion = effect->control (
        effect, VST_EFFECT_OPCODE_VENDOR_VERSION, 0, 0, nullptr, 0.f);

    std::cout << "{\"protocolVersion\":" << kProtocolVersion
              << ",\"pluginId\":\"" << jsonEscape (pluginId)
              << "\",\"sequence\":" << sequence
              << ",\"type\":\"metadata\",\"format\":\"vst2\""
              << ",\"name\":\"" << jsonEscape (name) << "\""
              << ",\"vendor\":\"" << jsonEscape (vendor) << "\""
              << ",\"product\":\"" << jsonEscape (product) << "\""
              << ",\"uniqueId\":\"" << jsonEscape (fourCc (effect->unique_id)) << "\""
              << ",\"version\":" << effect->version
              << ",\"vendorVersion\":" << vendorVersion
              << ",\"apiVersion\":" << apiVersion
              << ",\"category\":" << category
              << ",\"programs\":" << effect->num_programs
              << ",\"parameters\":" << effect->num_params
              << ",\"audioInputs\":" << effect->num_inputs
              << ",\"audioOutputs\":" << effect->num_outputs
              << ",\"hasEditor\":" << ((effect->flags & VST_EFFECT_FLAG_EDITOR) ? "true" : "false")
              << "}\n";
}

} // namespace

int scanVst2 (const std::string& pluginId, const wchar_t* path)
{
    std::uint64_t sequence = 1;
    emitState (pluginId, sequence++, "scanning");

    HMODULE module = LoadLibraryW (path);
    if (!module)
    {
        emitError (pluginId, sequence, "DLL_LOAD_FAILED", windowsError (GetLastError ()));
        return 10;
    }

    using EntryPoint = vst_effect_t* (VST_FUNCTION_INTERFACE*) (vst_host_callback_t);
    auto entryPoint = reinterpret_cast<EntryPoint> (GetProcAddress (module, "VSTPluginMain"));
    if (!entryPoint)
        entryPoint = reinterpret_cast<EntryPoint> (GetProcAddress (module, "main"));
    if (!entryPoint)
    {
        emitError (pluginId, sequence, "VST2_ENTRYPOINT_MISSING",
                   "The DLL exports neither VSTPluginMain nor the legacy main entry point");
        FreeLibrary (module);
        return 11;
    }

    vst_effect_t* effect = entryPoint (hostCallback);
    if (!effect || effect->magic_number != static_cast<std::int32_t> (VST_MAGICNUMBER) ||
        !effect->control)
    {
        emitError (pluginId, sequence, "INVALID_VST2_EFFECT",
                   "The entry point did not return a valid VST2 effect");
        FreeLibrary (module);
        return 12;
    }

    effect->control (effect, VST_EFFECT_OPCODE_INITIALIZE, 0, 0, nullptr, 0.f);
    emitMetadata (pluginId, sequence++, effect);
    effect->control (effect, VST_EFFECT_OPCODE_DESTROY, 0, 0, nullptr, 0.f);
    FreeLibrary (module);
    emitState (pluginId, sequence, "ready");
    return 0;
}
