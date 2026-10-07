/** 아직 만들지 않은 화면 자리에 넣는 임시 페이지 */
export default function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="card">
      <h1>{title}</h1>
      <p className="muted">준비 중인 화면입니다.</p>
    </section>
  )
}
