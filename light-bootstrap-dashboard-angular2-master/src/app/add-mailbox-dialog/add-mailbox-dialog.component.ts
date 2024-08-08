import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-add-mailbox-dialog',
  templateUrl: './add-mailbox-dialog.component.html',
  styleUrls: ['./add-mailbox-dialog.component.scss']
})
export class AddMailboxDialogComponent {
  email: string = '';
  password: string = '';

  constructor(public dialogRef: MatDialogRef<AddMailboxDialogComponent>) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onAdd(): void {
    this.dialogRef.close({ email: this.email, password: this.password });
  }
}
