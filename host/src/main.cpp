#include "vst2_scanner.h"
#include "vst2_editor.h"
#include "pluginterfaces/vst/ivstaudioprocessor.h"
#include "public.sdk/source/vst/hosting/module.h"

#include <cstdint>
#include <iostream>
#include <string>
#include <cwctype>

#include <windows.h>

namespace {

constexpr std::uint32_t kProtocolVersion = 1;

std::string utf8 (const wchar_t* value)
{
    if (!value)
        return {};
    const int required = WideCharToMultiByte (CP_UTF8, 0, value, -1, nullptr, 0, nullptr, nullptr);
    if (required <= 1)
        return {};
    std::string result (static_cast<std::size_t> (required), '\0');
    WideCharToMultiByte (CP_UTF8, 0, value, -1, result.data (), required, nullptr, nullptr);
    result.pop_back ();
    return result;
}

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

void emitMetadata (const std::string& pluginId, std::uint64_t sequence,
                   const VST3::Hosting::FactoryInfo& factory,
                   const VST3::Hosting::ClassInfo& info)
{
    std::cout << "{\"protocolVersion\":" << kProtocolVersion
              << ",\"pluginId\":\"" << jsonEscape (pluginId)
              << "\",\"sequence\":" << sequence
              << ",\"type\":\"metadata\""
              << ",\"classId\":\"" << info.ID ().toString () << "\""
              << ",\"name\":\"" << jsonEscape (info.name ()) << "\""
              << ",\"vendor\":\"" << jsonEscape (info.vendor ().empty () ? factory.vendor () : info.vendor ()) << "\""
              << ",\"version\":\"" << jsonEscape (info.version ()) << "\""
              << ",\"sdkVersion\":\"" << jsonEscape (info.sdkVersion ()) << "\""
              << ",\"category\":\"" << jsonEscape (info.category ()) << "\""
              << ",\"subCategories\":\"" << jsonEscape (info.subCategoriesString ()) << "\""
              << ",\"audioEffect\":" << (info.category () == kVstAudioEffectClass ? "true" : "false")
              << "}\n";
}

int scan (const std::string& pluginId, const std::string& path)
{
    std::uint64_t sequence = 1;
    emitState (pluginId, sequence++, "scanning");

    std::string error;
    auto module = VST3::Hosting::Module::create (path, error);
    if (!module)
    {
        emitError (pluginId, sequence, "MODULE_LOAD_FAILED", error);
        return 3;
    }

    const auto& pluginFactory = module->getFactory ();
    const auto factoryInfo = pluginFactory.info ();
    const auto classes = pluginFactory.classInfos ();
    if (classes.empty ())
    {
        emitError (pluginId, sequence, "NO_PLUGIN_CLASSES", "The module factory exposes no classes");
        return 4;
    }

    bool foundAudioEffect = false;
    for (const auto& info : classes)
    {
        emitMetadata (pluginId, sequence++, factoryInfo, info);
        foundAudioEffect = foundAudioEffect || info.category () == kVstAudioEffectClass;
    }
    if (!foundAudioEffect)
    {
        emitError (pluginId, sequence, "NO_AUDIO_EFFECT_CLASS", "The module contains no VST3 audio effect class");
        return 5;
    }

    emitState (pluginId, sequence, "ready");
    return 0;
}

} // namespace

int wmain (int argc, wchar_t* argv[])
{
    const std::wstring command (argc > 1 ? argv[1] : L"");
    if (argc != 4 || (command != L"scan" && command != L"editor"))
    {
        std::cerr << "usage: vst-bridge-host <scan|editor> <plugin-id> <plugin-path>\n";
        return 2;
    }
    const std::string pluginId = utf8 (argv[2]);
    std::wstring path (argv[3]);
    std::wstring extension;
    const auto dot = path.find_last_of (L".");
    if (dot != std::wstring::npos)
        extension = path.substr (dot);
    for (wchar_t& character : extension)
        character = static_cast<wchar_t> (towlower (character));
    if (extension == L".dll")
        return command == L"editor" ? openVst2Editor(pluginId, argv[3]) : scanVst2(pluginId, argv[3]);
    if (command == L"editor")
    {
        std::cerr << "VST3 editor hosting is not implemented yet; VST2 DLL editors are supported\n";
        return 6;
    }
    return scan(pluginId, utf8(argv[3]));
}
