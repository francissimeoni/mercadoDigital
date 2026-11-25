import axiosClient from "./axiosClient"
import type { AddItemToCarritoPayload, CarritoResponse } from "../types/carrito"

export async function addItemToCarrito(payload: AddItemToCarritoPayload): Promise<void> {
  await axiosClient.post("/carrito/items", payload)
}

export async function getCarrito(): Promise<CarritoResponse> {
  const response = await axiosClient.get<CarritoResponse>("/carrito")
  return response.data
}
