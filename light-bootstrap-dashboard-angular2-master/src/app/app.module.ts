import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';

import { AppRoutingModule } from './app.routing';
import { NavbarModule } from './shared/navbar/navbar.module';
import { FooterModule } from './shared/footer/footer.module';
import { SidebarModule } from './sidebar/sidebar.module';

import { AppComponent } from './app.component';

import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ListMailBoxComponent } from './list-mail-box/list-mail-box.component';
import { LoginDialogComponent } from './login-dialog/login-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { AddMailboxDialogComponent } from './add-mailbox-dialog/add-mailbox-dialog.component';
import { ProgressDialogComponent } from './progress-dialog/progress-dialog.component';
import { ArchiveComponent } from './archive/archive.component';
import { TopDomainsChartComponent } from './top-domains-chart/top-domains-chart.component';


@NgModule({
  imports: [
    BrowserAnimationsModule,
    FormsModule,
    RouterModule,
    HttpClientModule,
    NavbarModule,
    FooterModule,
    SidebarModule,
    AppRoutingModule,
    MatTooltipModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressBarModule

  ],
  declarations: [
    AppComponent,
    AdminLayoutComponent,
    LoginComponent,
    RegisterComponent,
    ListMailBoxComponent,
    LoginDialogComponent,
    AddMailboxDialogComponent,
    ProgressDialogComponent,
    ArchiveComponent,
    TopDomainsChartComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
