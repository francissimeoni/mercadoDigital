import axiosClient from "../../api/axiosClient"

export type PublicQuestion = { id: string; pregunta: string; respuesta: string | null; fechaPregunta?: string }
export type PublicQuestionInput = {
  productoId: string; revendedorId: string; nombreContacto?: string
  emailContacto?: string; telefonoContacto?: string; pregunta: string
}
export type PublicAvailabilityInput = Omit<PublicQuestionInput, "pregunta"> & { localidad?: string; observaciones?: string }

export async function getPublicQuestions(productoId: string, revendedorId: string) {
  return (await axiosClient.get<PublicQuestion[]>("/public/consultas-producto/preguntas", { params: { productoId, revendedorId } })).data
}
export async function createPublicQuestion(input: PublicQuestionInput) {
  return (await axiosClient.post<PublicQuestion>("/public/consultas-producto/preguntas", input)).data
}
export async function createAvailabilityRequest(input: PublicAvailabilityInput) {
  return (await axiosClient.post("/public/consultas-producto/disponibilidad", input)).data
}
