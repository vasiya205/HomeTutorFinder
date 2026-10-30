// Builds an iCalendar (.ics) file so a lesson can be added to Google / Outlook / Apple Calendar.
const stamp = (value) => {
  const d = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}T${pad(d.getUTCHours())}${pad(d.getUTCMinutes())}00Z`
}
const esc = (text = '') => String(text).replace(/\\/g, '\\\\').replace(/[,;]/g, (m) => `\\${m}`).replace(/\n/g, '\\n')

export function downloadLessonInvite(booking) {
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Home Tutor Finder//Lessons//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'BEGIN:VEVENT',
    `UID:booking-${booking.id}@hometutorfinder`,
    `DTSTAMP:${stamp(new Date())}`,
    `DTSTART:${stamp(booking.startDateTime)}`,
    `DTEND:${stamp(booking.endDateTime)}`,
    `SUMMARY:${esc(`${booking.subject} lesson - ${booking.tutorName} & ${booking.studentName}`)}`,
    `DESCRIPTION:${esc(`Home Tutor Finder booking #${booking.id}\nTutor: ${booking.tutorName}\nStudent: ${booking.studentName}\nNotes: ${booking.notes || '-'}`)}`,
    'BEGIN:VALARM',
    'TRIGGER:-PT30M',
    'ACTION:DISPLAY',
    'DESCRIPTION:Lesson starts in 30 minutes',
    'END:VALARM',
    'END:VEVENT',
    'END:VCALENDAR',
  ]
  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `lesson-${booking.id}.ics`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function googleCalendarLink(booking) {
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: `${booking.subject} lesson with ${booking.tutorName}`,
    dates: `${stamp(booking.startDateTime)}/${stamp(booking.endDateTime)}`,
    details: `Home Tutor Finder booking #${booking.id}`,
  })
  return `https://calendar.google.com/calendar/render?${params}`
}
