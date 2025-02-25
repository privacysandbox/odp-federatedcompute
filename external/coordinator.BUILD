# Repackage required java libraries from coordinator

java_library(
    name = "crypto_client",
    srcs = [
        "//java/com/google/scp/operator/cpio/cryptoclient:model/ErrorReason.java",
        "//java/com/google/scp/operator/cpio/cryptoclient:DecryptionKeyService.java",
        "//java/com/google/scp/operator/cpio/cryptoclient:EncryptionKeyFetchingService.java",
        "//java/com/google/scp/operator/cpio/cryptoclient:HttpEncryptionKeyFetchingService.java",
        "//java/com/google/scp/operator/cpio/cryptoclient:Annotations.java",
        "//java/com/google/scp/operator/cpio/cryptoclient:MultiPartyDecryptionKeyServiceImpl.java",
    ],
    deps = [
        "@maven//:com_google_crypto_tink_tink",
        "//coordinator/protos/keymanagement/shared/api/v1:java_proto",
        "//coordinator/protos/keymanagement/keyhosting/api/v1:java_proto",
        ":cloud_aead_selector",
        ":key_utils",
        ":error_utils",
        ":http_client_wrapper",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:com_google_inject_guice",
    ],
    visibility = ["//visibility:public"],
)

java_library(
    name = "gcp_credentials_helper",
    srcs = [
        "//java/com/google/scp/shared/clients/configclient:gcp/CredentialSource.java",
        "//java/com/google/scp/shared/clients/configclient:gcp/CredentialConfig.java",
        "//java/com/google/scp/shared/clients/configclient:gcp/CredentialsHelper.java",
        "//java/com/google/scp/shared/mapper:GuavaObjectMapper.java",
    ],
    deps = [
        ":autovalue",
        "@maven//:com_fasterxml_jackson_datatype_jackson_datatype_guava",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
    ],
    visibility = ["//visibility:public"],
)

java_library(
    name = "http_client_wrapper",
    srcs = [
        "//java/com/google/scp/shared/api/util:HttpClientWrapper.java",
        "//java/com/google/scp/shared/api/util:HttpClientResponse.java",
        "//java/com/google/scp/shared/gcp/util:GcpHttpInterceptorUtil.java",
    ],
    deps = [
        ":autovalue",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:io_github_resilience4j_resilience4j_core",
        "@maven//:io_github_resilience4j_resilience4j_retry",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:org_apache_httpcomponents_core5_httpcore5",
        "@maven//:org_apache_httpcomponents_core5_httpcore5_h2",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
    ],
    javacopts = ["-XepDisableAllChecks"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "key_utils",
    srcs = [
        "//java/com/google/scp/shared/util:KeyParams.java",
        "//java/com/google/scp/shared/util:KeySplitUtil.java",
        "//java/com/google/scp/shared/util:KeysetHandleSerializerUtil.java",
    ],
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_crypto_tink_tink",
    ],
    visibility = ["//visibility:public"],
)

java_library(
    name = "error_utils",
    srcs = [
        "//java/com/google/scp/shared/api/util:ErrorUtil.java",
        "//java/com/google/scp/shared/api/model:Code.java",
        "//java/com/google/scp/shared/api/exception:ServiceException.java",
        "//java/com/google/scp/shared/api/exception:SharedErrorReason.java",
    ],
    deps = [
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "//shared/protos/api/v1:java_proto",
    ],
    visibility = ["//visibility:public"],
)

java_library(
    name = "cloud_aead_selector",
    srcs = [
        "//java/com/google/scp/shared/crypto/tink:CloudAeadSelector.java",
    ],
    deps = [
        "@maven//:com_google_crypto_tink_tink",
    ],
    visibility = ["//visibility:public"],
)

java_plugin(
    name = "autovalue_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = ["@maven//:com_google_auto_value_auto_value"],
)

java_plugin(
    name = "autooneof_plugin",
    processor_class = "com.google.auto.value.processor.AutoOneOfProcessor",
    deps = ["@maven//:com_google_auto_value_auto_value"],
)

java_library(
    name = "autovalue",
    exported_plugins = [
        ":autooneof_plugin",
        ":autovalue_plugin",
    ],
    neverlink = True,
    exports = ["@maven//:com_google_auto_value_auto_value_annotations"],
)
