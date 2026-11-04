pub mod leetcodeauth {
    include!("proto/leetcodeauth.rs");
}

/// Client for `leetcode-auth-stealer-service` (LASS).
pub mod lass {
    include!("proto/codebloom.leetcode.auth.v1.rs");
}

pub const FILE_DESCRIPTOR_SET: &[u8] = include_bytes!("proto/descriptor.bin");
