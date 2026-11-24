import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {
  Atencion,
  ESTADO_LABELS,
  ESTADOS_ATENCION,
  EstadoAtencion,
  PRIORIDADES,
  PrioridadAtencion,
  TRANSICIONES_ESTADO,
  fechaHoraLocal,
} from '../../core/models/appointment.model';
import { AppointmentsService } from '../../core/services/appointments.service';
import { CatalogService } from '../../core/services/catalog.service';
import { RoleService } from '../../core/services/role.service';

/**
 * Valida que la fecha/hora ingresada sea posterior al momento actual.
 * Rechaza fechas pasadas o iguales a "ahora", que no constituyen una
 * atención agendable. Devuelve `fechaPasada` o `fechaInvalida`.
 */
const fechaFuturaValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const valor = control.value as string;
  if (!valor) {
    return null;
  }
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return { fechaInvalida: true };
  }
  return fecha.getTime() > Date.now() ? null : { fechaPasada: true };
};

/** Formato `YYYY-MM-DDTHH:mm` en hora local, requerido por `<input type="datetime-local">`. */
function ahoraLocalInput(): string {
  const ahora = new Date();
  ahora.setMinutes(ahora.getMinutes() - ahora.getTimezoneOffset());
  return ahora.toISOString().slice(0, 16);
}

/** RUT chileno con puntos y guion obligatorios, p. ej. 21.345.123-4. */
const RUT_REGEX = /^\d{1,3}\.\d{3}\.\d{3}-[\dkK]$/;

const rutFormatoValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = ((control.value as string) ?? '').trim();
  if (!valor) {
    return null;
  }
  return RUT_REGEX.test(valor) ? null : { rutFormato: true };
};

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-appointments',
  templateUrl: './appointments.html',
})
export class Appointments {
  private readonly appointments = inject(AppointmentsService);
  private readonly catalog = inject(CatalogService);
  private readonly fb = inject(FormBuilder);
  protected readonly roleService = inject(RoleService);

  protected readonly estados = ESTADOS_ATENCION;
  protected readonly estadoLabels = ESTADO_LABELS;
  protected readonly prioridades = PRIORIDADES;
  protected readonly prestaciones = this.catalog.prestaciones;
  protected readonly profesionales = this.catalog.profesionales;
  protected readonly boxes = this.catalog.boxes;
  protected readonly fechaHora = fechaHoraLocal;
  protected readonly porEstado = this.appointments.porEstado;
  protected readonly bitacora = this.appointments.bitacora;
  protected readonly total = this.appointments.total;

  protected readonly filtroEstado = signal<EstadoAtencion | ''>('');
  protected readonly filtroTexto = signal('');
  protected readonly mostrarFormulario = signal(false);
  protected readonly mensaje = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    paciente: ['', Validators.required],
    rut: ['', [Validators.required, rutFormatoValidator]],
    especialidad: ['', Validators.required],
    prestacionId: ['', Validators.required],
    profesional: ['', Validators.required],
    box: ['', Validators.required],
    fecha: ['', [Validators.required, fechaFuturaValidator]],
    prioridad: ['NORMAL', Validators.required],
    observaciones: [''],
  });

  /** Límite inferior del selector de fecha: el momento actual. */
  protected readonly fechaMinima = signal(ahoraLocalInput());

  /** Atenciones visibles tras aplicar los filtros de estado y texto. */
  protected readonly atenciones = computed<Atencion[]>(() => {
    const estado = this.filtroEstado();
    const texto = this.filtroTexto().trim().toLowerCase();

    return this.appointments.atenciones().filter((a) => {
      if (estado && a.estado !== estado) {
        return false;
      }
      if (texto) {
        const blob = `${a.id} ${a.paciente} ${a.rut} ${a.especialidad} ${a.profesional}`.toLowerCase();
        if (!blob.includes(texto)) {
          return false;
        }
      }
      return true;
    });
  });

  protected readonly puedeSolicitar = computed(() =>
    this.roleService.hasRole(['ADMIN', 'RECEPCIONISTA', 'PACIENTE'])
  );

  /** Solo Admin y Recepcionista pueden eliminar atenciones de la agenda. */
  protected readonly puedeEliminar = computed(() =>
    this.roleService.hasRole(['ADMIN', 'RECEPCIONISTA'])
  );

  protected cambiarFiltroEstado(valor: string): void {
    this.filtroEstado.set(valor as EstadoAtencion | '');
  }

  protected cambiarFiltroTexto(valor: string): void {
    this.filtroTexto.set(valor);
  }

  protected alternarFormulario(): void {
    this.mostrarFormulario.update((v) => !v);
    this.mensaje.set(null);
    if (this.mostrarFormulario()) {
      this.fechaMinima.set(ahoraLocalInput());
    }
  }

  protected crear(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      if (this.form.controls.rut.hasError('rutFormato')) {
        this.mensaje.set('El RUT debe tener el formato 21.345.123-4 (con puntos y guion).');
      } else if (this.form.controls.fecha.hasError('fechaPasada')) {
        this.mensaje.set('La fecha y hora de la atención debe ser posterior al momento actual.');
      } else {
        this.mensaje.set('Revisa los campos marcados antes de crear la atención.');
      }
      return;
    }

    const valor = this.form.getRawValue();
    const creada = this.appointments.crear(
      {
        paciente: valor.paciente,
        rut: valor.rut,
        especialidad: valor.especialidad,
        prestacionId: valor.prestacionId,
        profesional: valor.profesional,
        box: valor.box,
        fecha: valor.fecha,
        prioridad: valor.prioridad as PrioridadAtencion,
        observaciones: valor.observaciones,
      },
      this.roleService.usuarioActual()
    );

    this.mensaje.set(`Atención ${creada.id} creada y evento publicado al backend.`);
    this.form.reset({ prioridad: 'NORMAL' });
    this.mostrarFormulario.set(false);
  }

  protected cambiarEstado(id: string, nuevoEstado: EstadoAtencion): void {
    const ok = this.appointments.cambiarEstado(id, nuevoEstado);
    this.mensaje.set(
      ok
        ? `Atención ${id} actualizada a ${ESTADO_LABELS[nuevoEstado]} y evento/notificación disparados.`
        : `No fue posible cambiar el estado de la atención ${id}.`
    );
  }

  protected notificar(id: string): void {
    this.appointments.notificar(id);
    this.mensaje.set(`Notificación enviada al backend para la atención ${id}.`);
  }

  protected eliminar(id: string): void {
    if (!confirm(`¿Eliminar la atención ${id}? Se publicará el evento de anulación en Kafka.`)) {
      return;
    }
    const ok = this.appointments.eliminar(id);
    this.mensaje.set(
      ok ? `Atención ${id} eliminada de la agenda.` : `No se encontró la atención ${id}.`
    );
  }

  /** Indica si la atención fue solicitada por el usuario autenticado. */
  protected esPropia(atencion: Atencion): boolean {
    return (
      atencion.solicitanteEmail !== null &&
      atencion.solicitanteEmail === this.roleService.usuarioActual()
    );
  }

  /**
   * Transiciones habilitadas según el rol del usuario.
   * El paciente solo puede cancelar sus propias atenciones; Admin y Recepcionista
   * administran toda la agenda y el Auditor no modifica nada.
   */
  protected transicionesPermitidas(atencion: Atencion): EstadoAtencion[] {
    const permitidas = TRANSICIONES_ESTADO[atencion.estado];
    const rol = this.roleService.currentRole();

    if (rol === 'ADMIN' || rol === 'RECEPCIONISTA') {
      return permitidas;
    }
    if (rol === 'PACIENTE' && this.esPropia(atencion)) {
      return permitidas.filter((estado) => estado === 'CANCELADA');
    }
    return [];
  }

  /** true cuando el rol actual no puede ejecutar ninguna acción sobre la atención. */
  protected sinAcciones(atencion: Atencion): boolean {
    return (
      this.roleService.currentRole() === 'PACIENTE' &&
      this.transicionesPermitidas(atencion).length === 0
    );
  }

  protected etiquetaPrestacion(id: string): string {
    return this.prestaciones().find((p) => p.id === id)?.nombre ?? id;
  }

  protected estadoClase(estado: EstadoAtencion): string {
    return `badge-${estado.toLowerCase()}`;
  }

  protected prioridadClase(prioridad: PrioridadAtencion): string {
    return `badge-${prioridad.toLowerCase()}`;
  }

  protected limpiarBitacora(): void {
    this.appointments.limpiarBitacora();
  }
}
