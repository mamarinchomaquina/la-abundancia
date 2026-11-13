import { Component, OnInit, AfterViewInit, signal, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { UpperCasePipe } from '@angular/common';
import { AuthService } from '../../services/auth';
import { ApiService, Estadisticas, Graficas, mensajeDeError } from '../../services/api';
import { Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, UpperCasePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('chartBarras')  canvasBarras!: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartDonut')   canvasDonut!:  ElementRef<HTMLCanvasElement>;

  stats     = signal<Estadisticas | null>(null);
  graficas  = signal<Graficas | null>(null);
  cargando  = signal(true);
  error     = signal('');

  private chartBarras?: Chart;
  private chartDonut?:  Chart;

  constructor(
    public auth: AuthService,
    private api: ApiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.api.getEstadisticas().subscribe({
      next: (data) => { this.stats.set(data); this.cargando.set(false); },
      error: (e)    => { this.error.set(mensajeDeError(e, 'No se pudieron cargar las estadísticas.')); this.cargando.set(false); },
    });
    this.api.getGraficas().subscribe({
      next: (data) => { this.graficas.set(data); this.crearGraficas(); },
      error: ()     => {},
    });
  }

  ngAfterViewInit() {
    if (this.graficas()) this.crearGraficas();
  }

  crearGraficas() {
    const g = this.graficas();
    if (!g || !this.canvasBarras || !this.canvasDonut) return;
    setTimeout(() => { this.buildBarras(g); this.buildDonut(g); }, 50);
  }

  private buildBarras(g: Graficas) {
    this.chartBarras?.destroy();
    this.chartBarras = new Chart(this.canvasBarras.nativeElement, {
      type: 'bar',
      data: {
        labels: g.porSemana.map(s => s.label),
        datasets: [{
          label: 'Publicaciones',
          data: g.porSemana.map(s => s.total),
          backgroundColor: 'rgba(107,33,168,.18)',
          borderColor: '#6B21A8',
          borderWidth: 2,
          borderRadius: 6,
          borderSkipped: false,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#72726D' } },
          y: { beginAtZero: true, grid: { color: '#F0EEF5' }, ticks: { stepSize: 1, font: { size: 11 }, color: '#72726D' } },
        },
        animation: { duration: 800, easing: 'easeOutQuart' },
      },
    });
  }

  private buildDonut(g: Graficas) {
    this.chartDonut?.destroy();
    const colores = ['#6B21A8','#7C3AED','#A855F7','#C084FC','#E9D5FF'];
    this.chartDonut = new Chart(this.canvasDonut.nativeElement, {
      type: 'doughnut',
      data: {
        labels: g.porCategoria.map(c => c.categoria),
        datasets: [{
          data: g.porCategoria.map(c => c.total),
          backgroundColor: colores.slice(0, g.porCategoria.length),
          borderWidth: 0,
          hoverOffset: 8,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        cutout: '68%',
        plugins: {
          legend: { position: 'bottom', labels: { font: { size: 12 }, color: '#1A1A1F', padding: 14, usePointStyle: true, pointStyleWidth: 8 } },
        },
        animation: { duration: 900, easing: 'easeOutQuart' },
      },
    });
  }

  ngOnDestroy() {
    this.chartBarras?.destroy();
    this.chartDonut?.destroy();
  }

  cerrarSesion() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
