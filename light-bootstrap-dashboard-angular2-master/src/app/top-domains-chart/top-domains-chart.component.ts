import { Component, OnInit } from '@angular/core';
import { EmailService } from 'app/Services/email.service';
import { Chart, registerables } from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';

Chart.register(...registerables);
Chart.register(ChartDataLabels);

@Component({
  selector: 'app-top-domains-chart',
  templateUrl: './top-domains-chart.component.html',
  styleUrls: ['./top-domains-chart.component.scss']
})
export class TopDomainsChartComponent implements OnInit {
  chart: any;

  constructor(private emailService: EmailService) {}

  ngOnInit(): void {
    this.emailService.getTopDomainsWithAttachments().subscribe(data => {
      const domainNames = data.map((item: any) => item.domainName);
      const attachmentCounts = data.map((item: any) => item.attachmentCount);
  
      if (this.chart) {
        this.chart.destroy(); // Destroy the existing chart instance
      }
  
      this.chart = new Chart('canvas', {
        type: 'bar',
        data: {
          labels: domainNames,
          datasets: [
            {
              label: 'Number of Attachments',
              data: attachmentCounts,
              backgroundColor: domainNames.map((_, index) => `rgba(${(index * 50) % 255}, ${(index * 100) % 255}, ${(index * 150) % 255}, 0.6)`),
              borderColor: 'rgba(54, 162, 235, 1)',
              borderWidth: 1,
              hoverBackgroundColor: 'rgba(54, 162, 235, 0.8)',
              hoverBorderColor: 'rgba(54, 162, 235, 1)',
            }
          ]
        },
        options: {
          plugins: {
            title: {
              display: true,
              text: 'Top 10 Email Domains with Most Attachments',
              font: {
                size: 18
              }
            },
            legend: {
              display: false
            },
            tooltip: {
              callbacks: {
                label: function(tooltipItem) {
                  return ` ${tooltipItem.label}: ${tooltipItem.raw} attachments`;
                }
              }
            },
            datalabels: {
              color: '#000',
              anchor: 'end',
              align: 'top',
              formatter: (value) => value,
              font: {
                weight: 'bold'
              },
              offset: 4
            }
          },
          scales: {
            x: {
              title: {
                display: true,
                text: 'Domain Names',
                font: {
                  size: 14
                }
              },
              ticks: {
                font: {
                  size: 12
                },
                autoSkip: true,
                maxRotation: 45,
                minRotation: 45
              },
              grid: {
                display: false
              }
            },
            y: {
              beginAtZero: true,
              min: 0, // Set the minimum value for y-axis
              max: 70, // Set the maximum value to fit the data range
              title: {
                display: true,
                text: 'Number of Attachments',
                font: {
                  size: 14,
                  weight: 'bold'
                }
              },
              ticks: {
                font: {
                  size: 12,
                  weight: 'bold'
                },
                stepSize: 10, // Set the step size to 10
                callback: function(value) {
                  return value; // Format the y-axis labels
                }
              },
              grid: {
                color: 'rgba(0,0,0,0.1)',
                lineWidth: 1
              }
            }
          },
          responsive: true,
          maintainAspectRatio: false
        }
      });
    });
  }
}  