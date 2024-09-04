import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TopDomainsChartComponent } from './top-domains-chart.component';

describe('TopDomainsChartComponent', () => {
  let component: TopDomainsChartComponent;
  let fixture: ComponentFixture<TopDomainsChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TopDomainsChartComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TopDomainsChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
