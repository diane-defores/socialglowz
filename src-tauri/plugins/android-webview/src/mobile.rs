use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use tauri::{plugin::PluginHandle, Runtime};

use crate::error::{Error, Result};

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OpenRequest {
    pub url: String,
    pub account_id: String,
    pub network_id: String,
    pub storage_origins: Vec<String>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountRequest {
    pub account_id: String,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct ShowResponse {
    shown: bool,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GrayscaleRequest {
    pub enabled: bool,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DarkModeRequest {
    pub enabled: bool,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BarNetworksRequest {
    pub network_ids: Vec<String>,
    pub storage_origins_by_network_json: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SetProfilesRequest {
    pub profiles_json: String,
    pub active_profile_id: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SetLocaleRequest {
    pub locale: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TextZoomRequest {
    pub level: i32,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DeleteSessionRequest {
    pub profile_id: String,
    pub network_id: String,
}

pub struct AndroidWebview<R: Runtime>(pub PluginHandle<R>);

impl<R: Runtime> AndroidWebview<R> {
    pub fn open(
        &self,
        url: &str,
        account_id: &str,
        network_id: &str,
        storage_origins: Vec<String>,
    ) -> Result<()> {
        self.0
            .run_mobile_plugin(
                "openWebView",
                OpenRequest {
                    url: url.to_string(),
                    account_id: account_id.to_string(),
                    network_id: network_id.to_string(),
                    storage_origins,
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn close(&self, account_id: &str) -> Result<()> {
        self.0
            .run_mobile_plugin(
                "closeWebView",
                AccountRequest {
                    account_id: account_id.to_string(),
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn show(&self, account_id: &str) -> Result<bool> {
        let response: ShowResponse = self
            .0
            .run_mobile_plugin(
                "showWebView",
                AccountRequest {
                    account_id: account_id.to_string(),
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))?;
        Ok(response.shown)
    }

    pub fn hide(&self, account_id: &str) -> Result<()> {
        self.0
            .run_mobile_plugin(
                "hideWebView",
                AccountRequest {
                    account_id: account_id.to_string(),
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn set_grayscale(&self, enabled: bool) -> Result<()> {
        self.0
            .run_mobile_plugin("setGrayscale", GrayscaleRequest { enabled })
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn set_dark_mode(&self, enabled: bool) -> Result<()> {
        self.0
            .run_mobile_plugin("setDarkMode", DarkModeRequest { enabled })
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn set_text_zoom(&self, level: i32) -> Result<()> {
        self.0
            .run_mobile_plugin("setTextZoom", TextZoomRequest { level })
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn set_bar_networks(
        &self,
        network_ids: Vec<String>,
        storage_origins_by_network: HashMap<String, Vec<String>>,
    ) -> Result<()> {
        let storage_origins_by_network_json =
            serde_json::to_string(&storage_origins_by_network)
                .map_err(|e| Error::PluginInvoke(e.to_string()))?;
        self.0
            .run_mobile_plugin(
                "setBarNetworks",
                BarNetworksRequest {
                    network_ids,
                    storage_origins_by_network_json,
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn set_profiles(&self, profiles_json: String, active_profile_id: String) -> Result<()> {
        self.0
            .run_mobile_plugin(
                "setProfiles",
                SetProfilesRequest {
                    profiles_json,
                    active_profile_id,
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn set_locale(&self, locale: String) -> Result<()> {
        self.0
            .run_mobile_plugin("setLocale", SetLocaleRequest { locale })
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn delete_network_session(&self, profile_id: &str, network_id: &str) -> Result<()> {
        self.0
            .run_mobile_plugin(
                "deleteNetworkSession",
                DeleteSessionRequest {
                    profile_id: profile_id.to_string(),
                    network_id: network_id.to_string(),
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }

    pub fn delete_profile_session(&self, profile_id: &str) -> Result<()> {
        self.0
            .run_mobile_plugin(
                "deleteProfileSession",
                DeleteSessionRequest {
                    profile_id: profile_id.to_string(),
                    network_id: String::new(),
                },
            )
            .map_err(|e| Error::PluginInvoke(e.to_string()))
    }
}
