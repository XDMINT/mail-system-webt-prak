import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';

import { MailDrafts } from './mail-drafts';

describe('MailDrafts', () => {
  let component: MailDrafts;
  let fixture: ComponentFixture<MailDrafts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailDrafts],
      providers: [MessageService],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailDrafts);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
