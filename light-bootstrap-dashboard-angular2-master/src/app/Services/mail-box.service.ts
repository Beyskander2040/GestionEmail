import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class MailBoxService {
  private apiUrl = 'http://localhost:8088/MailBox/api/v1/auth/mailboxes';


  constructor(private http: HttpClient) { }
   // Fetch mailboxes by user ID
   getMailboxes(userId: number): Observable<any> {
    const token = localStorage.getItem('authToken');
    console.log('Token from localStorage:', localStorage.getItem('authToken'));

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  
    return this.http.get(`${this.apiUrl}/getMailBoxByuser`, { headers, params: { userId: userId.toString() } })
      .pipe(
        catchError(err => {
          console.error('Error fetching mailboxes', err);
          return throwError(err);
        })
      );
  }
  
}
