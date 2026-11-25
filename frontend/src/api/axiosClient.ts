import axios from "axios"
import { clearAuthSession, getAuthToken, redirectToLogin } from "../auth/authSession"
import { isStorefrontBrowserHost } from "../tenant/storefrontHost"
import { resolveApiBaseUrl } from "./resolveApiBaseUrl"

const axiosClient = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: 60_000,
})

function isPublicApiRequest(url: string): boolean {
  return url.includes("/public/") || url.includes("/auth/login")
}

function isStorefrontPublicPath(pathname: string): boolean {
  return (
    pathname === "/" ||
    pathname === "/catalogo" ||
    pathname === "/quienes-somos" ||
    pathname === "/login" ||
    pathname === "/ingresar" ||
    pathname === "/mi-tienda" ||
    pathname.startsWith("/producto/") ||
    pathname === "/pago/retorno" ||
    pathname.startsWith("/pedido/")
  )
}

axiosClient.interceptors.request.use((config) => {
  const token = getAuthToken()
  const requestUrl = typeof config.url === "string" ? config.url : ""

  if (token && !isPublicApiRequest(requestUrl)) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status
    const requestUrl = typeof error.config?.url === "string" ? error.config.url : ""
    const isLoginRequest = requestUrl.includes("/auth/login")
    const onStorefrontPublicPath =
      typeof window !== "undefined" &&
      isStorefrontBrowserHost(window.location.hostname) &&
      isStorefrontPublicPath(window.location.pathname)

    if (status === 401 && !isLoginRequest && !onStorefrontPublicPath) {
      clearAuthSession("expired")
      redirectToLogin()
    }

    return Promise.reject(error)
  },
)

export default axiosClient
