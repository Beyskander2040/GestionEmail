import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddMailboxDialogComponent } from './add-mailbox-dialog.component';

describe('AddMailboxDialogComponent', () => {
  let component: AddMailboxDialogComponent;
  let fixture: ComponentFixture<AddMailboxDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AddMailboxDialogComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddMailboxDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
