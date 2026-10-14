use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jint};
use jni::JNIEnv;
use photocraft_engine::Session;
use photocraft_io::{ExportOptions, export, import};
use serde_json::json;

fn adjust_png(input: &[u8], brightness: i32, contrast: i32) -> Result<Vec<u8>, String> {
    if input.is_empty() {
        return Err("请选择一张有效的 PNG/JPEG 图片。".to_string());
    }

    // Choose a matching extension because PhotoCraft's import pipeline uses the filename
    // to select a decoder. Android photo pickers commonly return JPEG or PNG bytes.
    let input_name = if input.starts_with(b"\x89PNG\r\n\x1a\n") {
        "mobile-input.png"
    } else if input.starts_with(&[0xFF, 0xD8, 0xFF]) {
        "mobile-input.jpg"
    } else if input.len() >= 12 && &input[0..4] == b"RIFF" && &input[8..12] == b"WEBP" {
        "mobile-input.webp"
    } else {
        return Err("目前原生验证版支持 PNG、JPEG 和 WebP 图片。".to_string());
    };
    let imported = import(input_name, input).map_err(|e| format!("PhotoCraft 无法读取图片：{e}"))?;
    let mut session = Session::new();
    session.add_document(imported.document, None);

    // Use the same command registry used by PhotoCraft's desktop and web frontends.
    session
        .execute(
            "image.adjustments.brightnessContrast",
            json!({
                "brightness": brightness.clamp(-150, 150),
                "contrast": contrast.clamp(-50, 100),
                "legacy": false
            }),
        )
        .map_err(|e| format!("PhotoCraft 修图命令执行失败：{e}"))?;

    let document = session.active().ok_or_else(|| "PhotoCraft 没有活动中的图片。".to_string())?;
    let exported = export(&document.doc, "png", &ExportOptions::default())
        .map_err(|e| format!("PhotoCraft 导出 PNG 失败：{e}"))?;
    Ok(exported.bytes)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_shunzhan96_photocraftmobile_NativePhotoCraft_adjustPng(
    mut env: JNIEnv,
    _class: JClass,
    input: JByteArray,
    brightness: jint,
    contrast: jint,
) -> jbyteArray {
    let result = (|| {
        let bytes = env.convert_byte_array(&input).map_err(|e| e.to_string())?;
        adjust_png(&bytes, brightness, contrast)
    })();

    match result {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(array) => array.into_raw(),
            Err(error) => {
                let _ = env.throw_new("java/lang/IllegalStateException", error.to_string());
                std::ptr::null_mut()
            }
        },
        Err(error) => {
            let _ = env.throw_new("java/lang/IllegalStateException", error);
            std::ptr::null_mut()
        }
    }
}
