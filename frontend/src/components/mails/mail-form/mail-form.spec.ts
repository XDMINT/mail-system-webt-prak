import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';

import { MailForm } from './mail-form';

describe('MailForm', () => {
  let component: MailForm;
  let fixture: ComponentFixture<MailForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailForm],
      providers: [MessageService, provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
