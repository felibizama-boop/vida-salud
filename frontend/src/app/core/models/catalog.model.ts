/**
 * Modelo del catálogo clínico: prestaciones, boxes y cupos de agenda.
 */
export interface Prestacion {
  id: string;
  nombre: string;
  categoria: string;
  duracionMin: number;
  precio: number;
  activa: boolean;
}

export interface NuevaPrestacion {
  nombre: string;
  categoria: string;
  duracionMin: number;
  precio: number;
}

export interface Box {
  id: string;
  nombre: string;
  ubicacion: string;
  tipo: string;
  activo: boolean;
}

export interface NuevoBox {
  nombre: string;
  ubicacion: string;
  tipo: string;
}

export interface Cupo {
  id: string;
  prestacionId: string;
  profesional: string;
  dia: string;
  hora: string;
  capacidad: number;
  reservados: number;
}

export interface NuevoCupo {
  prestacionId: string;
  profesional: string;
  dia: string;
  hora: string;
  capacidad: number;
}

export const CATEGORIAS_PRESTACION: readonly string[] = [
  'Consulta',
  'Procedimiento',
  'Examen',
  'Kinesiología',
  'Odontología',
];

export const TIPOS_BOX: readonly string[] = [
  'Consulta',
  'Procedimiento',
  'Toma de muestras',
  'Kinesiología',
];

export function formatoClp(valor: number): string {
  return valor.toLocaleString('es-CL', { style: 'currency', currency: 'CLP' });
}
