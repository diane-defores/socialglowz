chrome.runtime.onInstalled.addListener(async (opt) => {
  if (opt.reason === "install") {
    await chrome.storage.local.clear()

    chrome.tabs.create({
      active: true,
      // Open the setup page and append `?type=install` to the URL so frontend
      // can know if we need to show the install page or update page.
      url: chrome.runtime.getURL("src/ui/setup/index.html#/setup/install"),
    })
    return
  }

  if (opt.reason === "update") {
    chrome.tabs.create({
      active: true,
      url: chrome.runtime.getURL("src/ui/setup/index.html#/setup/update"),
    })
  }
})

export {}
