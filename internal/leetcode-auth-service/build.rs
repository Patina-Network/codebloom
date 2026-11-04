use std::path::PathBuf;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let out_dir = "src/proto";
    std::fs::create_dir_all(out_dir)?;

    let out_dir = PathBuf::from(out_dir);

    tonic_prost_build::configure()
        .build_client(false)
        .out_dir(&out_dir)
        .file_descriptor_set_path(out_dir.join("descriptor.bin"))
        .compile_protos(&["proto/v1/leetcode_auth.proto"], &["proto/"])?;

    tonic_prost_build::configure()
        .build_server(false)
        .out_dir(&out_dir)
        .compile_protos(
            &["../leetcode-auth-stealer-service/proto/LeetcodeAuthStealerService.proto"],
            &["../leetcode-auth-stealer-service/proto/"],
        )?;

    Ok(())
}
