import axios from "axios"
import axiosClient from "../api/axiosClient"
import { alternateWwwHost } from "./tenantHost"
import type { TenantStorefront } from "./tenantTypes"

async function fetchStorefrontByHost(host: string): Promise<TenantStorefront> {
  const response = await axiosClient.get<TenantStorefront>("/public/storefront/resolve", {
    params: { host },
    headers: {
      "Cache-Control": "no-cache",
      Pragma: "no-cache",
    },
  })

  return response.data
}

export async function resolveStorefront(host: string): Promise<TenantStorefront> {
  try {
    return await fetchStorefrontByHost(host)
  } catch (error) {
    if (!axios.isAxiosError(error) || error.response?.status !== 404) {
      throw error
    }

    const alternateHost = alternateWwwHost(host)
    if (!alternateHost || alternateHost === host) {
      throw error
    }

    return fetchStorefrontByHost(alternateHost)
  }
}
