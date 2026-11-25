import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios"
import { resolveApiBaseUrl } from "../../api/resolveApiBaseUrl"
import {
  clearStorefrontSession,
  getStorefrontSession,
  saveStorefrontSession,
  type StorefrontSession,
} from "../auth/storefrontSession"

type RetryableRequest = InternalAxiosRequestConfig & { _storefrontRetry?: boolean }

const apiBaseUrl = resolveApiBaseUrl()
const storefrontOwnerClient = axios.create({ baseURL: apiBaseUrl, timeout: 60_000 })
let refreshPromise: Promise<StorefrontSession> | null = null

function shouldRefresh(token: string) {
  try {
    const encodedPayload = token.split(".")[1]
    if (!encodedPayload) return false
    const normalized = encodedPayload.replace(/-/g, "+").replace(/_/g, "/")
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=")
    const payload = JSON.parse(window.atob(padded)) as { exp?: number }
    return typeof payload.exp === "number" && payload.exp * 1000 <= Date.now() + 120_000
  } catch {
    return false
  }
}

async function renewStorefrontSession() {
  const current = getStorefrontSession()
  if (!current?.refreshToken) {
    throw new Error("La sesión no dispone de renovación automática.")
  }

  if (!refreshPromise) {
    refreshPromise = axios
      .post<StorefrontSession>(
        "/public/storefront/auth/refresh",
        {
          storefrontHost: window.location.hostname,
          refreshToken: current.refreshToken,
        },
        { baseURL: apiBaseUrl, timeout: 30_000 },
      )
      .then((response) => {
        saveStorefrontSession(response.data)
        return response.data
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

function expireSessionAndRedirect() {
  clearStorefrontSession()
  if (window.location.pathname !== "/ingresar") {
    window.location.replace("/ingresar?motivo=sesion-expirada")
  }
}

storefrontOwnerClient.interceptors.request.use(async (config) => {
  let session = getStorefrontSession()
  if (session?.refreshToken && shouldRefresh(session.token)) {
    try {
      session = await renewStorefrontSession()
    } catch {
      expireSessionAndRedirect()
      throw new axios.Cancel("La sesión venció y no pudo renovarse.")
    }
  }
  if (session?.token) config.headers.Authorization = `Bearer ${session.token}`
  return config
})

storefrontOwnerClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const request = error.config as RetryableRequest | undefined
    if ((error.response?.status === 401 || error.response?.status === 403) && request && !request._storefrontRetry && getStorefrontSession()) {
      request._storefrontRetry = true
      try {
        const renewed = await renewStorefrontSession()
        request.headers.Authorization = `Bearer ${renewed.token}`
        return storefrontOwnerClient.request(request)
      } catch {
        expireSessionAndRedirect()
      }
    }
    return Promise.reject(error)
  },
)

export default storefrontOwnerClient
