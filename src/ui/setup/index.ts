import { i18n } from "@/utils/i18n"
import { notivue } from "@/utils/notifications"
import { pinia } from "@/utils/pinia"
import { appRouter } from "@/utils/router"
import { createApp } from "vue"
import App from "./app.vue"
import "./index.scss"
import { applyDisableCopyProtection } from "@/utils/disableCopyProtection"

applyDisableCopyProtection()

appRouter.addRoute({
  path: "/",
  alias: "/setup",
  redirect: "/setup/install",
})

const app = createApp(App).use(i18n).use(notivue).use(pinia).use(appRouter)

app.mount("#app")

export default app
