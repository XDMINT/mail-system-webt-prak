import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';

import { MailCreate } from './mail-create';

describe('MailCreate', () => {
  let component: MailCreate;
  let fixture: ComponentFixture<MailCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailCreate],
      providers: [MessageService, provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
