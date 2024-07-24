import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ListMailBoxComponent } from './list-mail-box.component';

describe('ListMailBoxComponent', () => {
  let component: ListMailBoxComponent;
  let fixture: ComponentFixture<ListMailBoxComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ListMailBoxComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ListMailBoxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
