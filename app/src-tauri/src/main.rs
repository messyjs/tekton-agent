// ⚔ Tekton Agent OS — reads ~/.tekton (sessions, projects, models) and relays chat.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]
use serde_json::Value;
use std::path::{Path, PathBuf};

fn tekton_home() -> PathBuf {
    if let Ok(h) = std::env::var("TEKTON_HOME") { return PathBuf::from(h); }
    dirs::home_dir().map(|h| h.join(".tekton")).unwrap_or_else(|| PathBuf::from("."))
}

/// Guard: every read must stay inside the Tekton home.
fn safe_path(p: &str) -> Result<PathBuf, String> {
    let home = tekton_home();
    let cand = Path::new(p);
    let abs = if cand.is_absolute() { cand.to_path_buf() } else { home.join(cand) };
    abs.canonicalize()
        .ok()
        .filter(|a| a.starts_with(&home))
        .ok_or_else(|| format!("outside tekton home: {}", p))
}

#[tauri::command]
fn tekton_home_path() -> String { tekton_home().to_string_lossy().into_owned() }

#[tauri::command]
fn list_sessions() -> Result<Vec<Value>, String> {
    let dir = tekton_home().join("sessions");
    let mut out = Vec::new();
    if let Ok(rd) = std::fs::read_dir(&dir) {
        let mut metas: Vec<(std::time::SystemTime, Value)> = Vec::new();
        for e in rd.flatten() {
            let p = e.path();
            if p.extension().and_then(|s| s.to_str()) != Some("json") { continue; }
            if let Ok(txt) = std::fs::read_to_string(&p) {
                if let Ok(mut v) = serde_json::from_str::<Value>(&txt) {
                    let m = std::fs::metadata(&p).and_then(|m| m.modified()).unwrap_or(std::time::SystemTime::UNIX_EPOCH);
                    if v.get("title").is_none() { v["title"] = Value::String("untitled".into()); }
                    metas.push((m, v));
                }
            }
        }
        metas.sort_by(|a, b| b.0.cmp(&a.0));
        out = metas.into_iter().map(|(_, v)| v).collect();
    }
    Ok(out)
}

#[tauri::command]
fn read_text(path: String) -> Result<String, String> {
    let p = safe_path(&path)?;
    std::fs::read_to_string(&p).map_err(|e| e.to_string())
}

#[tauri::command]
fn read_json(path: String) -> Result<Value, String> {
    let p = safe_path(&path)?;
    let txt = std::fs::read_to_string(&p).map_err(|e| e.to_string())?;
    serde_json::from_str(&txt).map_err(|e| e.to_string())
}

#[tauri::command]
async fn http_json(method: String, url: String, body: Option<Value>) -> Result<Value, String> {
    let client = reqwest::Client::new();
    let m = method.to_uppercase();
    let mut req = match m.as_str() {
        "GET" => client.get(&url),
        "POST" => client.post(&url),
        "PUT" => client.put(&url),
        "DELETE" => client.delete(&url),
        _ => return Err(format!("bad method {m}")),
    };
    if let Some(b) = body { req = req.json(&b); }
    let resp = req.send().await.map_err(|e| e.to_string())?;
    let status = resp.status().as_u16();
    let txt = resp.text().await.map_err(|e| e.to_string())?;
    let mut v = serde_json::from_str::<Value>(&txt).unwrap_or(Value::String(txt));
    if let Value::Object(map) = &mut v { map.insert("_status".into(), Value::from(status)); }
    Ok(v)
}

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            tekton_home_path, list_sessions, read_text, read_json, http_json
        ])
        .run(tauri::generate_context!())
        .expect("error while running Tekton Agent OS");
}
