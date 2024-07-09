extern crate prost_build;

fn main() {
    prost_build::compile_protos(
        &[
            "../protobuf/ws.proto",
            "../protobuf/targetdatabase.proto",
            "../protobuf/wsserver.proto",
            "../protobuf/wsclient.proto",
        ],
        &["../protobuf"],
    )
    .unwrap();
}
