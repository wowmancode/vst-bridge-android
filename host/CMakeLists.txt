cmake_minimum_required(VERSION 3.25)

project(vst_bridge_host VERSION 0.1.0 LANGUAGES CXX)

if(NOT WIN32)
    message(FATAL_ERROR "vst-bridge-host is currently a Windows-only Wine payload")
endif()

set(VST3_SDK_ROOT "" CACHE PATH "Path to a recursive checkout of Steinberg's VST3 SDK")
set(VST2_SDK_ROOT "" CACHE PATH "Path to Xaymar clean-room VST2 SDK")
if(NOT EXISTS "${VST2_SDK_ROOT}/include/vst.h")
    message(FATAL_ERROR "Set VST2_SDK_ROOT to a vst2sdk checkout")
endif()

if(NOT EXISTS "${VST3_SDK_ROOT}/CMakeLists.txt")
    message(FATAL_ERROR "Set VST3_SDK_ROOT to a recursive VST3 SDK checkout")
endif()

set(SMTG_ENABLE_VST3_PLUGIN_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SMTG_ENABLE_VST3_HOSTING_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SMTG_ENABLE_VSTGUI_SUPPORT OFF CACHE BOOL "" FORCE)
set(SMTG_RUN_VST_VALIDATOR OFF CACHE BOOL "" FORCE)

add_subdirectory("${VST3_SDK_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/vst3sdk" EXCLUDE_FROM_ALL)

add_executable(vst-bridge-host
    src/main.cpp
    src/vst2_scanner.cpp
    src/vst2_scanner.h
    src/vst2_editor.cpp
    src/vst2_editor.h
    "${VST3_SDK_ROOT}/public.sdk/source/vst/hosting/module_win32.cpp"
)

target_include_directories(vst-bridge-host PRIVATE "${VST2_SDK_ROOT}/include")

add_library(vst2-test-plugin MODULE tests/fake_vst2.cpp)
target_include_directories(vst2-test-plugin PRIVATE "${VST2_SDK_ROOT}/include")
target_compile_features(vst2-test-plugin PRIVATE cxx_std_17)
set_target_properties(vst2-test-plugin PROPERTIES PREFIX "" OUTPUT_NAME "vst2-test-plugin")

target_compile_features(vst-bridge-host PRIVATE cxx_std_17)
target_link_libraries(vst-bridge-host PRIVATE sdk_hosting)

if(MSVC)
    target_compile_options(vst-bridge-host PRIVATE /W4 /permissive-)
    set_target_properties(vst-bridge-host PROPERTIES LINK_FLAGS "/SUBSYSTEM:console")
endif()
