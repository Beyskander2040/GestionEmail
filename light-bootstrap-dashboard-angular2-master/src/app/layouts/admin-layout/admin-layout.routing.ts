import { Routes } from '@angular/router';

import { UserComponent } from '../../user/user.component';
import { MailComponent } from 'app/mail/mail.component';



export const AdminLayoutRoutes: Routes = [
    { path: 'user',component: UserComponent },
    { path: 'Mail', component: MailComponent },


    
   
];
