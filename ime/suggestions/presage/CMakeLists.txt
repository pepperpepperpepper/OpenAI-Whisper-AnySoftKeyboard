cmake_minimum_required(VERSION 3.22.1)

project(anysoftkeyboard_presage_bridge LANGUAGES CXX)

set(PRESAGE_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/presage/presage-0.9.1/src/lib)

set(PRESAGE_SOURCES
    ${PRESAGE_ROOT}/presage.cpp

    ${PRESAGE_ROOT}/core/combiner.cpp
    ${PRESAGE_ROOT}/core/configuration.cpp
    ${PRESAGE_ROOT}/core/defaultProfile.cpp
    ${PRESAGE_ROOT}/core/dispatcher.cpp
    ${PRESAGE_ROOT}/core/logger.cpp
    ${PRESAGE_ROOT}/core/meritocracyCombiner.cpp
    ${PRESAGE_ROOT}/core/observable.cpp
    ${PRESAGE_ROOT}/core/observer.cpp
    ${PRESAGE_ROOT}/core/prediction.cpp
    ${PRESAGE_ROOT}/core/predictorActivator.cpp
    ${PRESAGE_ROOT}/core/predictorRegistry.cpp
    ${PRESAGE_ROOT}/core/presageException.cpp
    ${PRESAGE_ROOT}/core/profile.cpp
    ${PRESAGE_ROOT}/core/profileManager.cpp
    ${PRESAGE_ROOT}/core/progress.cpp
    ${PRESAGE_ROOT}/core/selector.cpp
    ${PRESAGE_ROOT}/core/suggestion.cpp
    ${PRESAGE_ROOT}/core/utility.cpp
    ${PRESAGE_ROOT}/core/variable.cpp

    ${PRESAGE_ROOT}/core/context_tracker/contextChangeDetector.cpp
    ${PRESAGE_ROOT}/core/context_tracker/contextTracker.cpp

    ${PRESAGE_ROOT}/core/tokenizer/forwardTokenizer.cpp
    ${PRESAGE_ROOT}/core/tokenizer/reverseTokenizer.cpp
    ${PRESAGE_ROOT}/core/tokenizer/stringForwardTokenizer.cpp
    ${PRESAGE_ROOT}/core/tokenizer/tokenizer.cpp

    ${PRESAGE_ROOT}/predictors/ARPAPredictor.cpp
    ${PRESAGE_ROOT}/predictors/abbreviationExpansionPredictor.cpp
    ${PRESAGE_ROOT}/predictors/dejavuPredictor.cpp
    ${PRESAGE_ROOT}/predictors/dictionaryPredictor.cpp
    ${PRESAGE_ROOT}/predictors/dummyPredictor.cpp
    ${PRESAGE_ROOT}/predictors/predictor.cpp
    ${PRESAGE_ROOT}/predictors/recencyPredictor.cpp

    ${PRESAGE_ROOT}/tinyxml/tinystr.cpp
    ${PRESAGE_ROOT}/tinyxml/tinyxml.cpp
    ${PRESAGE_ROOT}/tinyxml/tinyxmlerror.cpp
    ${PRESAGE_ROOT}/tinyxml/tinyxmlparser.cpp)

add_library(presage_static STATIC ${PRESAGE_SOURCES})
target_include_directories(presage_static
    PUBLIC
        ${PRESAGE_ROOT}
        ${PRESAGE_ROOT}/core
        ${PRESAGE_ROOT}/core/context_tracker
        ${PRESAGE_ROOT}/core/tokenizer
        ${PRESAGE_ROOT}/predictors
        ${PRESAGE_ROOT}/predictors/dbconnector
        ${PRESAGE_ROOT}/tinyxml
        ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jni)
set_target_properties(presage_static PROPERTIES
    CXX_STANDARD 14
    CXX_STANDARD_REQUIRED YES
    CXX_EXTENSIONS NO)
target_compile_definitions(presage_static PRIVATE HAVE_CONFIG_H=1)

add_library(anysoftkeyboard_presage SHARED
    src/main/jni/presage_bridge.cpp)

set_target_properties(anysoftkeyboard_presage PROPERTIES
    CXX_STANDARD 14
    CXX_STANDARD_REQUIRED YES
    CXX_EXTENSIONS NO)
target_compile_options(anysoftkeyboard_presage PRIVATE -Wall -Wextra -Werror -Wno-ignored-qualifiers -fvisibility=hidden)

find_library(log-lib log)
target_link_libraries(anysoftkeyboard_presage PRIVATE presage_static ${log-lib})
