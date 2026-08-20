import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SideNavigation } from './side-navigation';

describe('SideNavigation', () => {
  let component: SideNavigation;
  let fixture: ComponentFixture<SideNavigation>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SideNavigation],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(SideNavigation);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
