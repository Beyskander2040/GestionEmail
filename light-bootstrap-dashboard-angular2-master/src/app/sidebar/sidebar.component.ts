import { Component, OnInit } from '@angular/core';

declare const $: any;
declare interface RouteInfo {
    path: string;
    title: string;
    icon: string;
    class: string;
}
export const ROUTES: RouteInfo[] = [
  
    // { path: '/Mail', title: ' Boite Mail',  icon:'pe-7s-note2', class: '' },
     { path: '/MesBoites', title: 'Mes Boites Mail',  icon:'pe-7s-note2', class: '' },
     { path: '/archive/:mailboxId', title: 'Archive',  icon:'fa fa-archive', class: '' },
     { path: '/statistiques', title: 'Statistiques', icon: 'bi bi-bar-chart', class: '' }


];

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent implements OnInit {
  menuItems: any[];

  constructor() { }

  ngOnInit() {
    this.menuItems = ROUTES.filter(menuItem => menuItem);
  }
  isMobileMenu() {
      if ($(window).width() > 991) {
          return false;
      }
      return true;
  };
}
