import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  CATEGORIAS_PRESTACION,
  TIPOS_BOX,
  formatoClp,
} from '../../core/models/catalog.model';
import { CatalogService } from '../../core/services/catalog.service';
import { RoleService } from '../../core/services/role.service';

type Pestana = 'prestaciones' | 'boxes' | 'cupos';

const DIAS_SEMANA: readonly string[] = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
];

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-catalog',
  templateUrl: './catalog.html',
})
export class Catalog {
  private readonly catalog = inject(CatalogService);
  private readonly fb = inject(FormBuilder);
  protected readonly roleService = inject(RoleService);

  protected readonly categorias = CATEGORIAS_PRESTACION;
  protected readonly tiposBox = TIPOS_BOX;
  protected readonly dias = DIAS_SEMANA;
  protected readonly clp = formatoClp;

  protected readonly prestaciones = this.catalog.prestaciones;
  protected readonly boxes = this.catalog.boxes;
  protected readonly cupos = this.catalog.cupos;
  protected readonly profesionales = this.catalog.profesionales;
  protected readonly prestacionesActivas = this.catalog.prestacionesActivas;
  protected readonly boxesActivos = this.catalog.boxesActivos;
  protected readonly cuposDisponibles = this.catalog.cuposDisponibles;
  protected readonly precioPromedio = this.catalog.precioPromedio;

  protected readonly pestana = signal<Pestana>('prestaciones');
  protected readonly mensaje = signal<string | null>(null);

  protected readonly puedeEditar = computed(() =>
    this.roleService.hasRole(['ADMIN', 'RECEPCIONISTA'])
  );

  protected readonly formPrestacion = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    categoria: [CATEGORIAS_PRESTACION[0], Validators.required],
    duracionMin: [20, [Validators.required, Validators.min(5)]],
    precio: [0, [Validators.required, Validators.min(0)]],
  });

  protected readonly formBox = this.fb.nonNullable.group({
    nombre: ['', Validators.required],
    ubicacion: ['', Validators.required],
    tipo: [TIPOS_BOX[0], Validators.required],
  });

  protected readonly formCupo = this.fb.nonNullable.group({
    prestacionId: ['', Validators.required],
    profesional: ['', Validators.required],
    dia: [DIAS_SEMANA[0], Validators.required],
    hora: ['09:00', Validators.required],
    capacidad: [1, [Validators.required, Validators.min(1)]],
  });

  protected cambiarPestana(pestana: Pestana): void {
    this.pestana.set(pestana);
    this.mensaje.set(null);
  }

  protected crearPrestacion(): void {
    if (this.formPrestacion.invalid) {
      this.formPrestacion.markAllAsTouched();
      return;
    }
    const valor = this.formPrestacion.getRawValue();
    this.catalog.agregarPrestacion({
      nombre: valor.nombre,
      categoria: valor.categoria,
      duracionMin: valor.duracionMin,
      precio: valor.precio,
    });
    this.mensaje.set(`Prestación "${valor.nombre}" agregada al catálogo.`);
    this.formPrestacion.reset({
      categoria: CATEGORIAS_PRESTACION[0],
      duracionMin: 20,
      precio: 0,
    });
  }

  protected crearBox(): void {
    if (this.formBox.invalid) {
      this.formBox.markAllAsTouched();
      return;
    }
    const valor = this.formBox.getRawValue();
    this.catalog.agregarBox(valor);
    this.mensaje.set(`Box "${valor.nombre}" creado.`);
    this.formBox.reset({ tipo: TIPOS_BOX[0] });
  }

  protected crearCupo(): void {
    if (this.formCupo.invalid) {
      this.formCupo.markAllAsTouched();
      return;
    }
    const valor = this.formCupo.getRawValue();
    this.catalog.agregarCupo(valor);
    this.mensaje.set(`Cupo creado para ${valor.dia} a las ${valor.hora}.`);
    this.formCupo.reset({
      dia: DIAS_SEMANA[0],
      hora: '09:00',
      capacidad: 1,
    });
  }

  protected actualizarPrecio(id: string, precio: number): void {
    if (Number.isNaN(precio) || precio < 0) {
      return;
    }
    this.catalog.actualizarPrecio(id, precio);
    this.mensaje.set(`Precio de ${id} actualizado.`);
  }

  protected alternarPrestacion(id: string): void {
    this.catalog.alternarPrestacion(id);
  }

  protected alternarBox(id: string): void {
    this.catalog.alternarBox(id);
  }

  protected reservarCupo(id: string): void {
    this.catalog.reservarCupo(id);
    this.mensaje.set(`Cupo ${id} reservado.`);
  }

  protected eliminarPrestacion(id: string, nombre: string): void {
    if (!confirm(`¿Eliminar la prestación "${nombre}" (${id})? También se eliminarán sus cupos.`)) {
      return;
    }
    this.catalog.eliminarPrestacion(id);
    this.mensaje.set(`Prestación "${nombre}" eliminada del catálogo.`);
  }

  protected eliminarBox(id: string, nombre: string): void {
    if (!confirm(`¿Eliminar el box "${nombre}" (${id})?`)) {
      return;
    }
    this.catalog.eliminarBox(id);
    this.mensaje.set(`Box "${nombre}" eliminado.`);
  }

  protected eliminarCupo(id: string): void {
    if (!confirm(`¿Eliminar el cupo ${id}?`)) {
      return;
    }
    this.catalog.eliminarCupo(id);
    this.mensaje.set(`Cupo ${id} eliminado.`);
  }

  protected ocupacion(capacidad: number, reservados: number): number {
    if (capacidad === 0) {
      return 0;
    }
    return Math.round((reservados / capacidad) * 100);
  }

  protected nombrePrestacion(id: string): string {
    return this.prestaciones().find((p) => p.id === id)?.nombre ?? id;
  }
}
