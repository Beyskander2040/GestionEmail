import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { LoginDialogComponent } from 'app/login-dialog/login-dialog.component';
import { Mailbox } from 'app/Models/mailbox';
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

  constructor(
    private mailboxService: MailBoxService,
    private router: Router,
    public dialog: MatDialog
  ) {}

  ngOnInit(): void {
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

  onSelectMailbox(mailbox: Mailbox): void {
    this.selectedMailbox = mailbox;
    const dialogRef = this.dialog.open(LoginDialogComponent, {
      width: '300px',
      data: { emailAddress: mailbox.emailAddress }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.email = result.email;
        this.password = result.password;
        this.router.navigate(['/Mail'], {
          queryParams: {
            email: this.email,
            password: this.password
          }
        });
      }
    });
  }
  onDeleteMailbox(mailbox: Mailbox): void {
    // Placeholder for delete functionality
    console.log('Delete mailbox:', mailbox);
  }
  
  onAddMailbox(): void {
    // Placeholder for add functionality
    console.log('Add new mailbox');
  }
}
