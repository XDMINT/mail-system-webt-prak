import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';

import { MailSent } from './mail-sent';

describe('MailSent', () => {
  let component: MailSent;
  let fixture: ComponentFixture<MailSent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailSent],
      providers: [MessageService],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailSent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
