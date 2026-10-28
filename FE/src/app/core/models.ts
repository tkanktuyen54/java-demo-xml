export interface User {
  id: number;
  name: string;
  email: string;
}
export interface Course {
  id: number;
  slug: string;
  title: string;
  subtitle: string;
  description: string;
  category: string;
  level: string;
  instructor: string;
  instructorTitle: string;
  price: number;
  originalPrice: number;
  hours: number;
  rating: number;
  studentCount: number;
  coverKey: string;
  badge: string | null;
  outcomes: string[];
  lessonCount: number;
}
export interface Lesson {
  id: number;
  title: string;
  chapter: string;
  position: number;
  durationMinutes: number;
  preview: boolean;
}
export interface CourseDetail {
  course: Course;
  lessons: Lesson[];
  enrolled: boolean;
}
export interface VideoLesson {
  id: number;
  title: string;
  durationMinutes: number;
  videoUrl: string;
  demo: boolean;
}
export interface Enrollment {
  orderId: number;
  slug: string;
  title: string;
  instructor: string;
  coverKey: string;
  lessonCount: number;
  completedLessons: number;
  amount: number;
  status: string;
}
