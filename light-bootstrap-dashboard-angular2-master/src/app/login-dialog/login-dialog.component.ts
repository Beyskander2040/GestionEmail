import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Mail } from 'app/Models/mail';
import { EmailService } from 'app/Services/email.service';

@Component({
  selector: 'app-login-dialog',
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.scss']
})
export class LoginDialogComponent {
  email: string = '';
  password: string = '';
  loading: boolean = false;
  emails: any[] = [];
  page: number = 0;
  size: number = 10;

  constructor(
    public dialogRef: MatDialogRef<LoginDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private emailService: EmailService,
    private sanitizer: DomSanitizer,
    private router: Router
  ) {
    this.email = data.emailAddress;
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  onFetchEmails(): void {
    this.loading = true;
    this.emailService.getEmails(this.email, this.password, this.page, this.size).subscribe(
      (data: Mail[]) => {
        const emailsWithSafeContent = data.map(email => ({
          mail: email,
          safeContent: this.sanitizer.bypassSecurityTrustHtml(email.content),
          fullContentVisible: false
        }));
        this.emails = [...this.emails, ...emailsWithSafeContent];
        this.loading = false;

        // Close the dialog and return email, password, and email data
        this.dialogRef.close({
          email: this.email,
          password: this.password,
          emails: emailsWithSafeContent
        });
      },
      (error) => {
        console.error('Error fetching emails:', error);
        this.loading = false;
      }
    );
  }
}
