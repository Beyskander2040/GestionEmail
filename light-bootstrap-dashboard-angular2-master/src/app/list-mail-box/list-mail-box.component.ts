import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AddMailboxDialogComponent } from 'app/add-mailbox-dialog/add-mailbox-dialog.component';
import { LoginDialogComponent } from 'app/login-dialog/login-dialog.component';
import { Mailbox } from 'app/Models/mailbox';
import { ProgressDialogComponent } from 'app/progress-dialog/progress-dialog.component';
import { EmailService } from 'app/Services/email.service';
import { MailBoxService } from 'app/Services/mail-box.service';
import { jwtDecode } from 'jwt-decode';

@Component({
  selector: 'app-list-mail-box',
  templateUrl: './list-mail-box.component.html',
  styleUrls: ['./list-mail-box.component.scss']
})
export class ListMailBoxComponent implements OnInit {
  mailboxes: Mailbox[] = [];
  selectedMailbox: Mailbox | null = null;
  email: string = '';
  password: string = '';
  error: string | null = null;
  progress: number = 0;
  isCreatingMailbox: boolean = false;

  constructor(
    private mailboxService: MailBoxService,
    private mailservice: EmailService,

    private router: Router,
    public dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadMailboxes();
    this.progress = 50;
  }

  private getUserIdFromToken(): number | null {
    const token = localStorage.getItem('authToken');
    if (token) {
      try {
        const decodedToken: any = jwtDecode(token);
        return decodedToken.userId || null;
      } catch (error) {
        console.error('Failed to decode token', error);
        return null;
      }
    }
    return null;
  }

  private loadMailboxes(): void {
    const userId = this.getUserIdFromToken();
    if (userId) {
      this.mailboxService.getMailboxes(userId).subscribe(
        data => {
          this.mailboxes = data;
        },
        error => {
          this.error = 'Failed to load mailboxes';
          console.error('Error:', error);
        }
      );
    } else {
      this.error = 'User ID not found in token';
    }
  }
  onSelectMailbox(mailbox: Mailbox): void {
    this.selectedMailbox = mailbox;
    const dialogRef = this.dialog.open(LoginDialogComponent, {
      width: '300px',
      data: { emailAddress: mailbox.emailAddress,password:mailbox.password, mailboxId: mailbox.id }
    });
  
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log('Dialog closed with result:', result);
        this.email = result.email;
        this.password = result.password;
        this.router.navigate(['/Mail'], {
          queryParams: {
            email: this.email,
            password: this.password,
            mailboxId: result.mailboxId
          }
        }).then(() => {
          console.log('Navigated to MailComponent');
        }).catch(err => {
          console.error('Navigation error:', err);
        });
      }
    });
  }
  

  onDeleteMailbox(mailboxId: number, event: Event): void {
    event.stopPropagation(); // Prevent triggering row click
    if (confirm('Are you sure you want to delete this mailbox?')) {
      this.mailboxService.deleteMailbox(mailboxId).subscribe(
        () => {
          // Successfully deleted mailbox, refresh the list
          this.loadMailboxes();
        },
        error => {
          this.error = 'Failed to delete mailbox';
          console.error('Error:', error);
        }
      );
    }
  }

  onAddMailbox(): void {
    const dialogRef = this.dialog.open(AddMailboxDialogComponent, {
      width: '300px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const { email, password } = result;
        const userId = this.getUserIdFromToken();
        if (userId) {
          const progressDialogRef = this.dialog.open(ProgressDialogComponent, {
            width: '400px',
            data: { progress: 0 }
          });

          this.mailboxService.createMailbox(email, password).subscribe(
            response => {
              this.loadMailboxes(); // Refresh mailboxes after adding

              const newMailboxId = response.body?.id; // Adjust based on your response structure
              if (newMailboxId) {
                this.trackProgress(newMailboxId, progressDialogRef);
              } else {
                this.error = 'Mailbox ID not found in response';
                progressDialogRef.close();
              }
            },
            error => {
              this.error = 'Failed to create mailbox. Please check your credentials.';
              progressDialogRef.close();
            }
          );
        }
      }
    });
  }

  trackProgress(mailboxId: number, progressDialogRef: any): void {
    this.mailboxService.getProgress(mailboxId).subscribe(
      (progress: number) => {
        progressDialogRef.componentInstance.progress = progress;
        if (progress === 100) {
          progressDialogRef.close();
        }
      },
      error => {
        progressDialogRef.close();
      }
    );
  }

}